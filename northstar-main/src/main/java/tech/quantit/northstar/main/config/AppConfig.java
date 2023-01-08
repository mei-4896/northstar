package tech.quantit.northstar.main.config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.corundumstudio.socketio.SocketIOServer;
import com.google.common.io.Files;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import tech.quantit.northstar.common.IMailMessageContentHandler;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.api.domain.mktdata.MarketCenter;
import tech.quantit.northstar.main.ExternalJarClassLoader;
import tech.quantit.northstar.main.SpringContextUtil;
import tech.quantit.northstar.main.interceptor.AuthorizationInterceptor;
import tech.quantit.northstar.main.mail.MailDeliveryManager;
import tech.quantit.northstar.main.mail.MailSenderFactory;
import tech.quantit.northstar.main.utils.ContractDefinitionReader;
import tech.quantit.northstar.main.utils.ModuleFactory;

/**
 * 配置转换器
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
@Configuration
public class AppConfig implements WebMvcConfigurer, DisposableBean {

	@Autowired
	private SocketIOServer socketServer;
	
	@Value("${northstar.contraceDefFile}")
	private Resource contractDefRes;
	
	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		/**
		 * 调整转换器优先级
		 */
		List<HttpMessageConverter<?>> jacksonConverters = new ArrayList<>();
		Iterator<HttpMessageConverter<?>> itCvt = converters.iterator();
		while (itCvt.hasNext()) {
			HttpMessageConverter<?> cvt = itCvt.next();
			if (cvt instanceof MappingJackson2HttpMessageConverter) {
				jacksonConverters.add(cvt);
				itCvt.remove();
			}
		}
		for (HttpMessageConverter<?> cvt : jacksonConverters) {
			converters.add(0, cvt);
		}
	}

	@Bean
	public CorsFilter corsFilter() {

		CorsConfiguration config = new CorsConfiguration();
		// 设置允许跨域请求的域名
		config.addAllowedOriginPattern("*");
		// 是否允许证书 不再默认开启
		config.setAllowCredentials(true);
		// 设置允许的方法
		config.addAllowedMethod("*");
		// 允许任何头
		config.addAllowedHeader("*");
		config.addExposedHeader("token");
		UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
		configSource.registerCorsConfiguration("/**", config);
		return new CorsFilter(configSource);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AuthorizationInterceptor()).addPathPatterns("/**")
				.excludePathPatterns("/auth/login");
	}

	@Bean
	public GatewayAndConnectionManager gatewayAndConnectionManager() {
		return new GatewayAndConnectionManager();
	}

	@Bean
	public ConcurrentMap<String, TradeDayAccount> accountMap() {
		return new ConcurrentHashMap<>();
	}

	@Bean
	public IMarketCenter marketCenter(FastEventEngine fastEventEngine) throws IOException {
		ContractDefinitionReader reader = new ContractDefinitionReader();
		return new MarketCenter(reader.load(contractDefRes.getInputStream()), fastEventEngine);
	}

	@Bean
	public ExternalJarClassLoader extJarListener(SpringContextUtil springContextUtil) throws MalformedURLException {
		ApplicationHome appHome = new ApplicationHome(getClass());
		File appPath = appHome.getDir();
		ExternalJarClassLoader clzLoader = null;
		for (File file : appPath.listFiles()) {
			if (file.getName().contains("northstar-external")
					&& Files.getFileExtension(file.getName()).equalsIgnoreCase("jar") && !file.isDirectory()) {
				log.info("加载northstar-external扩展包");
				clzLoader = new ExternalJarClassLoader(new URL[] { file.toURI().toURL() }, getClass().getClassLoader());
				clzLoader.initBean();
				break;
			}
		}
		return clzLoader;
	}

	@Bean
	public ModuleFactory moduleFactory(@Autowired(required = false) ExternalJarClassLoader extJarLoader, IGatewayRepository gatewayRepo,
			IModuleRepository moduleRepo, IMarketDataRepository mdRepo, GatewayAndConnectionManager gatewayConnMgr, IContractManager contractMgr,
			MailDeliveryManager mailMgr) {
		return new ModuleFactory(extJarLoader, moduleRepo, gatewayRepo, gatewayConnMgr, contractMgr, mailMgr);
	}

	private static OkHttpClient getUnsafeOkHttpClient() {
		try {
			// Create a trust manager that does not validate certificate chains
			final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[] {};
				}
			} };

			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
			return new OkHttpClient.Builder()
					.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
					.hostnameVerifier((host, session) -> true)
					.retryOnConnectionFailure(true)
					.build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplateBuilder()
				.requestFactory(() -> new OkHttp3ClientHttpRequestFactory(getUnsafeOkHttpClient()))
				.setReadTimeout(Duration.ofSeconds(30))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@ConditionalOnMissingBean(IMailMessageContentHandler.class)
	@Bean
	public IMailMessageContentHandler messageDeliveryHandler() {
		return new IMailMessageContentHandler() {
		};
	}

	@Bean
	public MailDeliveryManager mailDeliveryManager(IMailMessageContentHandler handler) {
		return new MailDeliveryManager(new MailSenderFactory(), handler);
	}

	@Override
	public void destroy() throws Exception {
		socketServer.stop();		
	}

}
