package com.uzima.bootstrap.config;

import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.application.message.port.out.MessageNotificationPort;
import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.application.workspace.port.out.ProjectRepositoryPort;
import com.uzima.application.workspace.port.out.TaskRepositoryPort;
import com.uzima.application.workspace.port.out.TimeEntryRepositoryPort;
import com.uzima.application.invoice.port.out.InvoiceRepositoryPort;
import com.uzima.application.qrcode.port.out.GeolocationPort;
import com.uzima.application.qrcode.port.out.CalendarIntegrationPort;
import com.uzima.application.message.port.out.VoiceTranscriptionPort;
import com.uzima.application.message.port.out.TranslationPort;
import com.uzima.application.message.port.out.IntentDetectionPort;
import com.uzima.application.message.port.out.EmotionAnalysisPort;
import com.uzima.application.message.port.out.ConversationSummaryPort;
import com.uzima.application.assistant.port.out.ReminderRepositoryPort;
import com.uzima.application.wellbeing.port.out.FocusSessionRepositoryPort;
import com.uzima.application.wellbeing.port.out.UsageSessionRepositoryPort;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import com.uzima.domain.payment.port.WalletRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.infrastructure.persistence.payment.SpringDataWalletRepository;
import com.uzima.infrastructure.persistence.payment.WalletRepositoryAdapter;
import com.uzima.application.notification.NotificationRouter;
import com.uzima.application.notification.NotificationRoutingStrategy;
import com.uzima.application.security.LoginAttemptRepositoryPort;
import com.uzima.application.qrcode.port.out.QrCodeRepositoryPort;
import com.uzima.application.user.port.out.PhoneValidationPort;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.bootstrap.adapter.http.security.InMemoryRateLimitStrategy;
import com.uzima.bootstrap.adapter.http.security.RateLimitingFilter;
import com.uzima.bootstrap.adapter.http.security.RateLimitStrategy;
import com.uzima.bootstrap.adapter.http.security.RedisRateLimitStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.uzima.application.notification.WebSocketNotifierPort;
import com.uzima.infrastructure.notification.*;
import com.uzima.infrastructure.persistence.message.ConversationRepositoryAdapter;
import com.uzima.infrastructure.persistence.message.SpringDataConversationRepository;
import com.uzima.infrastructure.payment.gateway.DisabledPaymentGatewayAdapter;
import com.uzima.infrastructure.persistence.message.MessageRepositoryAdapter;
import com.uzima.infrastructure.persistence.message.SpringDataMessageRepository;
import com.uzima.infrastructure.payment.gateway.CompositePaymentGatewayAdapter;
import com.uzima.infrastructure.payment.gateway.CryptoGatewayAdapter;
import com.uzima.infrastructure.payment.gateway.MobileMoneyGatewayAdapter;
import com.uzima.infrastructure.payment.gateway.StripeGatewayAdapter;
import com.uzima.infrastructure.payment.gateway.WalletGatewayAdapter;
import com.uzima.infrastructure.persistence.payment.SpringDataTransactionRepository;
import com.uzima.infrastructure.persistence.payment.TransactionRepositoryAdapter;
import com.uzima.infrastructure.persistence.invoice.InvoiceRepositoryAdapter;
import com.uzima.infrastructure.persistence.invoice.SpringDataInvoiceRepository;
import com.uzima.infrastructure.ai.StubGeolocationAdapter;
import com.uzima.infrastructure.ai.StubCalendarAdapter;
import com.uzima.infrastructure.ai.StubTranscriptionAdapter;
import com.uzima.infrastructure.ai.StubTranslationAdapter;
import com.uzima.infrastructure.ai.StubIntentDetectionAdapter;
import com.uzima.infrastructure.ai.StubEmotionAnalysisAdapter;
import com.uzima.infrastructure.ai.StubConversationSummaryAdapter;
import com.uzima.infrastructure.ai.OpenAIIntentDetectionAdapter;
import com.uzima.infrastructure.ai.OpenAITranslationAdapter;
import com.uzima.infrastructure.ai.OpenAIConversationSummaryAdapter;
import com.uzima.infrastructure.ai.OpenAITranscriptionAdapter;
import com.uzima.infrastructure.ai.GoogleCalendarAdapter;
import com.uzima.infrastructure.ai.IpGeolocationAdapter;
import com.uzima.infrastructure.ai.NeutralEmotionAnalysisAdapter;
import com.uzima.infrastructure.persistence.qrcode.QrCodeRepositoryAdapter;
import com.uzima.infrastructure.persistence.qrcode.SpringDataQrCodeRepository;
import com.uzima.infrastructure.persistence.assistant.ReminderRepositoryAdapter;
import com.uzima.infrastructure.persistence.assistant.SpringDataReminderRepository;
import com.uzima.infrastructure.persistence.wellbeing.FocusSessionRepositoryAdapter;
import com.uzima.infrastructure.persistence.wellbeing.SpringDataFocusSessionRepository;
import com.uzima.infrastructure.persistence.wellbeing.UsageSessionRepositoryAdapter;
import com.uzima.infrastructure.persistence.wellbeing.SpringDataUsageSessionRepository;
import com.uzima.infrastructure.persistence.social.CircleRepositoryAdapter;
import com.uzima.infrastructure.persistence.social.SpringDataCircleRepository;
import com.uzima.infrastructure.persistence.workspace.ProjectRepositoryAdapter;
import com.uzima.infrastructure.persistence.workspace.SpringDataProjectRepository;
import com.uzima.infrastructure.persistence.workspace.SpringDataTaskRepository;
import com.uzima.infrastructure.persistence.workspace.SpringDataTimeEntryRepository;
import com.uzima.infrastructure.persistence.workspace.TaskRepositoryAdapter;
import com.uzima.infrastructure.persistence.workspace.TimeEntryRepositoryAdapter;
import com.uzima.infrastructure.persistence.token.JpaRefreshTokenRepositoryAdapter;
import com.uzima.infrastructure.persistence.token.SpringDataRefreshTokenRepository;
import com.uzima.infrastructure.persistence.user.SpringDataUserRepository;
import com.uzima.infrastructure.persistence.user.UserRepositoryAdapter;
import com.uzima.infrastructure.security.InMemoryLoginAttemptRepository;
import com.uzima.infrastructure.security.LibPhoneNumberAdapter;
import com.uzima.infrastructure.security.SecureRefreshTokenHasher;
import com.uzima.security.token.port.RefreshTokenHasherPort;
import com.uzima.security.token.port.RefreshTokenRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration Spring : Instanciation des adaptateurs d'infrastructure.
 * <p>
 * Inclut :
 * - Repositories (JPA)
 * - Notification (Strategy pattern câblé ici)
 * - Sécurité (phone validation, token storage, rate limiting)
 */
@Configuration
@EnableScheduling
public class InfrastructureConfiguration {

    // -------------------------------------------------------------------------
    // Persistance - Users
    // -------------------------------------------------------------------------

    @Bean
    public UserRepositoryPort userRepositoryPort(SpringDataUserRepository springDataUserRepository) {
        return new UserRepositoryAdapter(springDataUserRepository);
    }

    @Bean
    public MessageRepositoryPort messageRepositoryPort(SpringDataMessageRepository springDataMessageRepository) {
        return new MessageRepositoryAdapter(springDataMessageRepository);
    }

    @Bean
    public ConversationRepositoryPort conversationRepositoryPort(
            SpringDataConversationRepository springDataConversationRepository) {
        return new ConversationRepositoryAdapter(springDataConversationRepository);
    }

    @Bean
    public QrCodeRepositoryPort qrCodeRepositoryPort(SpringDataQrCodeRepository springDataQrCodeRepository) {
        return new QrCodeRepositoryAdapter(springDataQrCodeRepository);
    }

    // -------------------------------------------------------------------------
    // Persistance - Circles (Social)
    // -------------------------------------------------------------------------

    @Bean
    public CircleRepositoryPort circleRepositoryPort(SpringDataCircleRepository springDataCircleRepository) {
        return new CircleRepositoryAdapter(springDataCircleRepository);
    }

    // -------------------------------------------------------------------------
    // Persistance - Workspace
    // -------------------------------------------------------------------------

    @Bean
    public ProjectRepositoryPort projectRepositoryPort(
            SpringDataProjectRepository projectJpa,
            SpringDataTaskRepository taskJpa,
            SpringDataTimeEntryRepository timeEntryJpa
    ) {
        return new ProjectRepositoryAdapter(projectJpa, taskJpa, timeEntryJpa);
    }

    @Bean
    public TaskRepositoryPort taskRepositoryPort(SpringDataTaskRepository springDataTaskRepository) {
        return new TaskRepositoryAdapter(springDataTaskRepository);
    }

    @Bean
    public TimeEntryRepositoryPort timeEntryRepositoryPort(SpringDataTimeEntryRepository springDataTimeEntryRepository) {
        return new TimeEntryRepositoryAdapter(springDataTimeEntryRepository);
    }

    // -------------------------------------------------------------------------
    // Persistance - Invoice
    // -------------------------------------------------------------------------

    @Bean
    public InvoiceRepositoryPort invoiceRepositoryPort(SpringDataInvoiceRepository springDataInvoiceRepository) {
        return new InvoiceRepositoryAdapter(springDataInvoiceRepository);
    }

    // -------------------------------------------------------------------------
    // Géolocalisation & Calendrier
    // -------------------------------------------------------------------------

    @Bean
    @Profile("!prod")
    public GeolocationPort geolocationPort() {
        return new StubGeolocationAdapter();
    }

    @Bean
    @Profile("prod")
    public GeolocationPort geolocationPortProd() {
        return new IpGeolocationAdapter();
    }

    @Bean
    @Profile("!prod")
    public CalendarIntegrationPort calendarIntegrationPort() {
        return new StubCalendarAdapter();
    }

    @Bean
    @Profile("prod")
    public CalendarIntegrationPort calendarIntegrationPortProd() {
        return new GoogleCalendarAdapter();
    }

    // -------------------------------------------------------------------------
    // IA — Adaptateurs (stubs en dev, OpenAI en prod)
    // -------------------------------------------------------------------------

    @Bean
    @Profile("!prod")
    public VoiceTranscriptionPort voiceTranscriptionPort() {
        return new StubTranscriptionAdapter();
    }

    @Bean
    @Profile("prod")
    public VoiceTranscriptionPort voiceTranscriptionPortProd(
            @Value("${uzima.ai.openai.api-key}") String apiKey
    ) {
        return new OpenAITranscriptionAdapter(apiKey);
    }

    @Bean
    @Profile("!prod")
    public TranslationPort translationPort() {
        return new StubTranslationAdapter();
    }

    @Bean
    @Profile("prod")
    public TranslationPort translationPortProd(
            @Value("${uzima.ai.openai.api-key}") String apiKey
    ) {
        return new OpenAITranslationAdapter(apiKey);
    }

    @Bean
    @Profile("!prod")
    public IntentDetectionPort intentDetectionPort() {
        return new StubIntentDetectionAdapter();
    }

    @Bean
    @Profile("prod")
    public IntentDetectionPort intentDetectionPortProd(
            @Value("${uzima.ai.openai.api-key}") String apiKey
    ) {
        return new OpenAIIntentDetectionAdapter(apiKey);
    }

    @Bean
    @Profile("!prod")
    public EmotionAnalysisPort emotionAnalysisPort() {
        return new StubEmotionAnalysisAdapter();
    }

    @Bean
    @Profile("prod")
    public EmotionAnalysisPort emotionAnalysisPortProd() {
        return new NeutralEmotionAnalysisAdapter();
    }

    @Bean
    @Profile("!prod")
    public ConversationSummaryPort conversationSummaryPort() {
        return new StubConversationSummaryAdapter();
    }

    @Bean
    @Profile("prod")
    public ConversationSummaryPort conversationSummaryPortProd(
            @Value("${uzima.ai.openai.api-key}") String apiKey
    ) {
        return new OpenAIConversationSummaryAdapter(apiKey);
    }

    // -------------------------------------------------------------------------
    // Persistance - Assistant IA (Reminders)
    // -------------------------------------------------------------------------

    @Bean
    public ReminderRepositoryPort reminderRepositoryPort(SpringDataReminderRepository springDataReminderRepository) {
        return new ReminderRepositoryAdapter(springDataReminderRepository);
    }

    // -------------------------------------------------------------------------
    // Persistance - Wellbeing
    // -------------------------------------------------------------------------

    @Bean
    public FocusSessionRepositoryPort focusSessionRepositoryPort(SpringDataFocusSessionRepository springDataFocusSessionRepository) {
        return new FocusSessionRepositoryAdapter(springDataFocusSessionRepository);
    }

    @Bean
    public UsageSessionRepositoryPort usageSessionRepositoryPort(SpringDataUsageSessionRepository springDataUsageSessionRepository) {
        return new UsageSessionRepositoryAdapter(springDataUsageSessionRepository);
    }

    // -------------------------------------------------------------------------
    // Persistance - Transactions (Payment)
    // -------------------------------------------------------------------------

    @Bean
    public TransactionRepositoryPort transactionRepositoryPort(
            SpringDataTransactionRepository springDataTransactionRepository
    ) {
        return new TransactionRepositoryAdapter(springDataTransactionRepository);
    }

    @Bean
    public WalletRepositoryPort walletRepositoryPort(SpringDataWalletRepository springDataWalletRepository) {
        return new WalletRepositoryAdapter(springDataWalletRepository);
    }

    /**
     * Gateway de paiement DÉSACTIVÉE — profil local.
     * Aucun appel réseau réel. Toutes les transactions retournent un succès simulé.
     */
    @Bean
    @Profile("local")
    public PaymentGatewayPort paymentGatewayPortLocal() {
        return new DisabledPaymentGatewayAdapter();
    }

    /**
     * Gateway de paiement composite — profil prod (MTN MoMo live + Stripe live keys).
     * Transactions RÉELLES. Débits effectifs.
     */
    @Bean
    @Profile("prod")
    public PaymentGatewayPort paymentGatewayPortProd(
            @Value("${uzima.payment.mobile-money.subscription-key}") String momoSubscriptionKey,
            @Value("${uzima.payment.mobile-money.api-user}")         String momoApiUser,
            @Value("${uzima.payment.mobile-money.api-key}")          String momoApiKey,
            @Value("${uzima.payment.mobile-money.base-url}")         String momoBaseUrl,
            @Value("${uzima.payment.mobile-money.callback-url}")     String momoCallbackUrl,
            @Value("${uzima.payment.mobile-money.environment}")      String momoEnvironment,
            @Value("${uzima.payment.stripe.secret-key}")             String stripeSecretKey,
            WalletRepositoryPort walletRepositoryPort,
            TimeProvider timeProvider
    ) {
        return buildCompositeGateway(
            momoBaseUrl, momoSubscriptionKey, momoApiUser, momoApiKey, momoCallbackUrl, momoEnvironment,
            stripeSecretKey, walletRepositoryPort, timeProvider
        );
    }

    private PaymentGatewayPort buildCompositeGateway(
            String momoBaseUrl, String momoSubscriptionKey, String momoApiUser,
            String momoApiKey, String momoCallbackUrl, String momoEnvironment,
            String stripeSecretKey, WalletRepositoryPort walletRepositoryPort, TimeProvider timeProvider
    ) {
        Map<PaymentMethod, PaymentGatewayPort> gateways = new EnumMap<>(PaymentMethod.class);
        gateways.put(PaymentMethod.MOBILE_MONEY, new MobileMoneyGatewayAdapter(
                momoBaseUrl, momoSubscriptionKey, momoApiUser, momoApiKey, momoCallbackUrl, momoEnvironment
        ));
        gateways.put(PaymentMethod.CARD,   new StripeGatewayAdapter(stripeSecretKey));
        gateways.put(PaymentMethod.WALLET, new WalletGatewayAdapter(walletRepositoryPort, timeProvider));
        gateways.put(PaymentMethod.CRYPTO, new CryptoGatewayAdapter());
        return new CompositePaymentGatewayAdapter(gateways);
    }

    // -------------------------------------------------------------------------
    // Persistance - Refresh Tokens
    // -------------------------------------------------------------------------

    @Bean
    public RefreshTokenRepositoryPort refreshTokenRepositoryPort(
            SpringDataRefreshTokenRepository springDataRefreshTokenRepository
    ) {
        return new JpaRefreshTokenRepositoryAdapter(springDataRefreshTokenRepository);
    }

    // -------------------------------------------------------------------------
    // Sécurité - Phone validation (libphonenumber)
    // -------------------------------------------------------------------------

    @Bean
    public PhoneValidationPort phoneValidationPort() {
        return new LibPhoneNumberAdapter();
    }

    // -------------------------------------------------------------------------
    // Sécurité - Refresh Token Hasher
    // -------------------------------------------------------------------------

    @Bean
    public RefreshTokenHasherPort refreshTokenHasherPort() {
        return new SecureRefreshTokenHasher();
    }

    // -------------------------------------------------------------------------
    // Sécurité - Brute force (login attempts)
    // -------------------------------------------------------------------------

    @Bean
    public LoginAttemptRepositoryPort loginAttemptRepositoryPort() {
        // Dev/Test : in-memory. Production : remplacer par RedisLoginAttemptRepository
        return new InMemoryLoginAttemptRepository();
    }

    // -------------------------------------------------------------------------
    // Notification (Strategy pattern + WebSocketNotifierPort)
    // -------------------------------------------------------------------------

    /**
     * Adaptateur WebSocket local/test : log structuré, aucune dépendance externe.
     * Actif sur tous les profils SAUF prod (SocketIOConfiguration fournit le bean en prod).
     */
    @Bean
    @Profile("!prod")
    public WebSocketNotifierPort webSocketNotifierPort() {
        return new LoggingNotificationAdapter();
    }

    @Bean
    public ImmediateNotificationStrategy immediateNotificationStrategy(WebSocketNotifierPort webSocketNotifierPort) {
        return new ImmediateNotificationStrategy(webSocketNotifierPort);
    }

    @Bean
    public DeferredNotificationStrategy deferredNotificationStrategy() {
        return new DeferredNotificationStrategy();
    }

    @Bean
    public BlockedNotificationStrategy blockedNotificationStrategy() {
        return new BlockedNotificationStrategy();
    }

    @Bean
    public UrgentOnlyNotificationStrategy urgentOnlyNotificationStrategy(
            ImmediateNotificationStrategy immediate,
            DeferredNotificationStrategy deferred
    ) {
        return new UrgentOnlyNotificationStrategy(immediate, deferred);
    }

    @Bean
    public NotificationRouter notificationRouter(
            ImmediateNotificationStrategy immediate,
            DeferredNotificationStrategy deferred,
            UrgentOnlyNotificationStrategy urgentOnly,
            BlockedNotificationStrategy blocked
    ) {
        List<NotificationRoutingStrategy> strategies = List.of(immediate, deferred, urgentOnly, blocked);
        return new NotificationRouter(strategies, immediate);
    }

    @Bean
    public MessageNotificationPort messageNotificationPort(
            NotificationRouter router,
            UserRepositoryPort userRepositoryPort
    ) {
        return new PresenceAwareNotificationAdapter(router, userRepositoryPort);
    }

    @Bean
    public DeferredQueueFlusherService deferredQueueFlusherService(
            DeferredNotificationStrategy deferredStrategy,
            WebSocketNotifierPort webSocketNotifierPort
    ) {
        return new DeferredQueueFlusherService(deferredStrategy, webSocketNotifierPort);
    }


    // -------------------------------------------------------------------------
    // HTTP - Rate limiting : stratégies
    // -------------------------------------------------------------------------

    /**
     * Rate limiting en mémoire — utilisé pour tous les profils (local + prod).
     * Suffisant pour une instance unique (déploiement solo).
     * Si tu passes en multi-instances à l'avenir, activer le profil "prod-cluster"
     * et configurer Redis.
     */
    @Bean
    @Profile("!prod-cluster")
    public RateLimitStrategy inMemoryRateLimitStrategy() {
        return new InMemoryRateLimitStrategy();
    }

    /**
     * Rate limiting Redis — uniquement si tu déploies plusieurs instances en parallèle.
     * Activer : SPRING_PROFILES_ACTIVE=prod,prod-cluster
     * Nécessite : REDIS_HOST, REDIS_PASSWORD définis.
     */
    @Bean
    @Profile("prod-cluster")
    public RateLimitStrategy redisRateLimitStrategy(StringRedisTemplate redisTemplate) {
        return new RedisRateLimitStrategy(redisTemplate);
    }

    // -------------------------------------------------------------------------
    // HTTP - Rate limiting : filtre
    // -------------------------------------------------------------------------

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter(
            RateLimitStrategy rateLimitStrategy,
            @Value("${uzima.security.rate-limit.max-requests:20}")       int    maxRequests,
            @Value("${uzima.security.rate-limit.window-seconds:60}")     int    windowSeconds,
            @Value("${uzima.security.rate-limit.path-prefix:/api/auth}") String pathPrefix,
            @Value("${uzima.security.rate-limit.waf-score-header:X-WAF-Score}") String wafScoreHeader,
            @Value("${uzima.security.rate-limit.waf-score-threshold:50}") int   wafScoreThreshold
    ) {
        RateLimitingFilter filter = new RateLimitingFilter(
            rateLimitStrategy, maxRequests, windowSeconds,
            pathPrefix, wafScoreHeader, wafScoreThreshold
        );
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns(pathPrefix + "/*");
        registration.setOrder(1);
        registration.setName("rateLimitingFilter");
        return registration;
    }
}
