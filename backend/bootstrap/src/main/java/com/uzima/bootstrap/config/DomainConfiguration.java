package com.uzima.bootstrap.config;

import com.uzima.application.payment.CancelTransactionUseCase;
import com.uzima.application.payment.GetTransactionHistoryUseCase;
import com.uzima.application.payment.RequestPaymentUseCase;
import com.uzima.application.payment.SendPaymentUseCase;
import com.uzima.application.payment.port.out.TransactionRepositoryPort;
import com.uzima.application.invoice.CancelInvoiceUseCase;
import com.uzima.application.invoice.CreateInvoiceUseCase;
import com.uzima.application.invoice.GetInvoiceUseCase;
import com.uzima.application.invoice.MarkInvoicePaidUseCase;
import com.uzima.application.invoice.SendInvoiceUseCase;
import com.uzima.application.invoice.port.out.InvoiceRepositoryPort;
import com.uzima.application.assistant.CreateReminderUseCase;
import com.uzima.application.assistant.TriggerReminderUseCase;
import com.uzima.application.assistant.SnoozeReminderUseCase;
import com.uzima.application.assistant.DismissReminderUseCase;
import com.uzima.application.assistant.GetRemindersUseCase;
import com.uzima.application.assistant.port.out.ReminderRepositoryPort;
import com.uzima.application.wellbeing.StartFocusSessionUseCase;
import com.uzima.application.wellbeing.EndFocusSessionUseCase;
import com.uzima.application.wellbeing.InterruptFocusSessionUseCase;
import com.uzima.application.wellbeing.TrackAppUsageUseCase;
import com.uzima.application.wellbeing.GetFocusSessionHistoryUseCase;
import com.uzima.application.wellbeing.GetUsageSessionsByAppTypeUseCase;
import com.uzima.application.wellbeing.GetWellbeingReportUseCase;
import com.uzima.application.wellbeing.port.out.FocusSessionRepositoryPort;
import com.uzima.application.wellbeing.port.out.UsageSessionRepositoryPort;
import com.uzima.application.social.AddMemberToCircleUseCase;
import com.uzima.application.social.CreateCircleUseCase;
import com.uzima.application.social.GetMyCirclesUseCase;
import com.uzima.application.social.RemoveMemberFromCircleUseCase;
import com.uzima.application.social.RenameCircleUseCase;
import com.uzima.application.social.SuggestCircleForContactUseCase;
import com.uzima.application.social.UpdateCircleRulesUseCase;
import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.application.workspace.CreateProjectUseCase;
import com.uzima.application.workspace.GetProjectsByMemberUseCase;
import com.uzima.application.workspace.GetRunningTimeEntryUseCase;
import com.uzima.application.workspace.GetTasksByAssigneeUseCase;
import com.uzima.application.workspace.GetTasksByStatusUseCase;
import com.uzima.application.workspace.CreateTaskUseCase;
import com.uzima.application.workspace.GetKanbanBoardUseCase;
import com.uzima.application.workspace.GetTimeReportUseCase;
import com.uzima.application.workspace.TrackTimeUseCase;
import com.uzima.application.workspace.UpdateTaskStatusUseCase;
import com.uzima.application.workspace.port.out.ProjectRepositoryPort;
import com.uzima.application.workspace.port.out.TaskRepositoryPort;
import com.uzima.application.workspace.port.out.TimeEntryRepositoryPort;
import com.uzima.application.message.*;
import com.uzima.application.message.port.out.*;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import com.uzima.application.qrcode.*;
import com.uzima.application.qrcode.port.out.QrCodeRepositoryPort;
import com.uzima.application.qrcode.port.out.GeolocationPort;
import com.uzima.application.qrcode.port.out.CalendarIntegrationPort;
import com.uzima.application.user.GetUserByIdUseCase;
import com.uzima.application.user.UpdateUserProfileUseCase;
import com.uzima.application.security.BruteForceProtectionService;
import com.uzima.application.security.LoginAttemptRepositoryPort;
import com.uzima.application.user.AuthenticateUserUseCase;
import com.uzima.application.user.RegisterUserUseCase;
import com.uzima.application.user.UpdatePresenceStatusUseCase;
import com.uzima.application.user.port.out.PasswordHasherPort;
import com.uzima.application.user.port.out.PhoneValidationPort;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.security.AccountLockoutPolicy;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.infrastructure.security.BcryptPasswordHasher;
import com.uzima.infrastructure.security.JwtAccessTokenGenerator;
import com.uzima.infrastructure.security.JwtAccessTokenVerifier;
import com.uzima.infrastructure.time.SystemTimeProvider;
import com.uzima.security.token.port.AccessTokenGeneratorPort;
import com.uzima.security.token.port.AccessTokenVerifierPort;
import com.uzima.security.token.port.RefreshTokenHasherPort;
import com.uzima.security.token.port.RefreshTokenRepositoryPort;
import com.uzima.security.token.usecase.GenerateTokenPairUseCase;
import com.uzima.security.token.usecase.GetActiveSessionsUseCase;
import com.uzima.security.token.usecase.IntrospectTokenUseCase;
import com.uzima.security.token.usecase.RefreshAccessTokenUseCase;
import com.uzima.security.token.usecase.RevokeAllTokensUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration Spring : Câblage de tous les composants (Wiring).
 * <p>
 * Principe DIP visible ici :
 * - Les Use Cases reçoivent des PORTS (interfaces du domaine/application/security)
 * - Les Beans instanciés sont des ADAPTATEURS (implémentations infrastructure)
 * - Changer une implémentation = modifier uniquement ce fichier
 */
@Configuration
public class DomainConfiguration {

    // -------------------------------------------------------------------------
    // Infrastructure - Adapters techniques partagés
    // -------------------------------------------------------------------------

    @Bean
    public TimeProvider timeProvider() {
        return new SystemTimeProvider();
    }

    @Bean
    public PasswordHasherPort passwordHasher() {
        return new BcryptPasswordHasher();
    }

    @Bean
    public AccessTokenGeneratorPort accessTokenGenerator(
            @Value("${uzima.security.jwt.secret}") String secret,
            @Value("${uzima.security.jwt.access-token-expiration-seconds:900}") long expirationSeconds
    ) {
        return new JwtAccessTokenGenerator(secret, Duration.ofSeconds(expirationSeconds));
    }

    @Bean
    public AccessTokenVerifierPort accessTokenVerifier(
            @Value("${uzima.security.jwt.secret}") String secret
    ) {
        return new JwtAccessTokenVerifier(secret);
    }

    // -------------------------------------------------------------------------
    // Sécurité - Politique de verrouillage (domaine pur)
    // -------------------------------------------------------------------------

    @Bean
    public AccountLockoutPolicy accountLockoutPolicy() {
        return new AccountLockoutPolicy();
    }

    @Bean
    public BruteForceProtectionService bruteForceProtectionService(
            AccountLockoutPolicy lockoutPolicy,
            LoginAttemptRepositoryPort loginAttemptRepository,
            TimeProvider timeProvider
    ) {
        return new BruteForceProtectionService(lockoutPolicy, loginAttemptRepository, timeProvider);
    }

    // -------------------------------------------------------------------------
    // Use Cases - Tokens (module security)
    // -------------------------------------------------------------------------

    @Bean
    public GenerateTokenPairUseCase generateTokenPairUseCase(
            AccessTokenGeneratorPort accessTokenGenerator,
            RefreshTokenHasherPort refreshTokenHasher,
            RefreshTokenRepositoryPort refreshTokenRepository,
            TimeProvider timeProvider
    ) {
        return new GenerateTokenPairUseCase(
                accessTokenGenerator, refreshTokenHasher, refreshTokenRepository, timeProvider
        );
    }

    @Bean
    public RefreshAccessTokenUseCase refreshAccessTokenUseCase(
            AccessTokenGeneratorPort accessTokenGenerator,
            RefreshTokenHasherPort refreshTokenHasher,
            RefreshTokenRepositoryPort refreshTokenRepository,
            TimeProvider timeProvider
    ) {
        return new RefreshAccessTokenUseCase(
                accessTokenGenerator, refreshTokenHasher, refreshTokenRepository, timeProvider
        );
    }

    @Bean
    public RevokeAllTokensUseCase revokeAllTokensUseCase(RefreshTokenRepositoryPort refreshTokenRepository) {
        return new RevokeAllTokensUseCase(refreshTokenRepository);
    }

    @Bean
    public IntrospectTokenUseCase introspectTokenUseCase(
            AccessTokenVerifierPort accessTokenVerifier,
            TimeProvider timeProvider
    ) {
        return new IntrospectTokenUseCase(accessTokenVerifier, timeProvider);
    }

    @Bean
    public GetActiveSessionsUseCase getActiveSessionsUseCase(
            RefreshTokenRepositoryPort refreshTokenRepository,
            TimeProvider timeProvider
    ) {
        return new GetActiveSessionsUseCase(refreshTokenRepository, timeProvider);
    }

    // -------------------------------------------------------------------------
    // Use Cases - User
    // -------------------------------------------------------------------------

    @Bean
    public RegisterUserUseCase registerUserUseCase(
            UserRepositoryPort userRepository,
            PasswordHasherPort passwordHasher,
            PhoneValidationPort phoneValidator,
            TimeProvider timeProvider
    ) {
        return new RegisterUserUseCase(userRepository, passwordHasher, phoneValidator, timeProvider);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(
            UserRepositoryPort userRepository,
            PasswordHasherPort passwordHasher,
            BruteForceProtectionService bruteForceProtectionService,
            TimeProvider timeProvider
    ) {
        return new AuthenticateUserUseCase(
                userRepository, passwordHasher, bruteForceProtectionService, timeProvider
        );
    }

    @Bean
    public UpdatePresenceStatusUseCase updatePresenceStatusUseCase(
            UserRepositoryPort userRepository,
            TimeProvider timeProvider
    ) {
        return new UpdatePresenceStatusUseCase(userRepository, timeProvider);
    }

    // -------------------------------------------------------------------------
    // Use Cases - Payment
    // -------------------------------------------------------------------------

    @Bean
    public SendPaymentUseCase sendPaymentUseCase(
            TransactionRepositoryPort transactionRepository,
            PaymentGatewayPort paymentGateway,
            TimeProvider timeProvider
    ) {
        return new SendPaymentUseCase(transactionRepository, paymentGateway, timeProvider);
    }

    @Bean
    public RequestPaymentUseCase requestPaymentUseCase(
            TransactionRepositoryPort transactionRepository,
            TimeProvider timeProvider
    ) {
        return new RequestPaymentUseCase(transactionRepository, timeProvider);
    }

    @Bean
    public GetTransactionHistoryUseCase getTransactionHistoryUseCase(
            TransactionRepositoryPort transactionRepository
    ) {
        return new GetTransactionHistoryUseCase(transactionRepository);
    }

    @Bean
    public CancelTransactionUseCase cancelTransactionUseCase(
            TransactionRepositoryPort transactionRepository,
            TimeProvider timeProvider
    ) {
        return new CancelTransactionUseCase(transactionRepository, timeProvider);
    }

    // -------------------------------------------------------------------------
    // Use Cases - Message
    // -------------------------------------------------------------------------

    @Bean
    public SendMessageUseCase sendMessageUseCase(
            ConversationRepositoryPort conversationRepository,
            MessageRepositoryPort messageRepository,
            MessageNotificationPort notificationPort,
            UserRepositoryPort userRepository,
            TimeProvider timeProvider
    ) {
        return new SendMessageUseCase(
                conversationRepository, messageRepository, notificationPort, userRepository, timeProvider
        );
    }

    @Bean
    public GetConversationUseCase getConversationUseCase(
            ConversationRepositoryPort conversationRepository,
            MessageRepositoryPort messageRepository
    ) {
        return new GetConversationUseCase(conversationRepository, messageRepository);
    }

    @Bean
    public StartConversationUseCase startConversationUseCase(
            ConversationRepositoryPort conversationRepository,
            UserRepositoryPort userRepository,
            TimeProvider timeProvider
    ) {
        return new StartConversationUseCase(conversationRepository, userRepository, timeProvider);
    }

    @Bean
    public GetUserConversationsUseCase getUserConversationsUseCase(
            ConversationRepositoryPort conversationRepository
    ) {
        return new GetUserConversationsUseCase(conversationRepository);
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserRepositoryPort userRepository) {
        return new GetUserByIdUseCase(userRepository);
    }

    @Bean
    public UpdateUserProfileUseCase updateUserProfileUseCase(UserRepositoryPort userRepository) {
        return new UpdateUserProfileUseCase(userRepository);
    }

    // -------------------------------------------------------------------------
    // Use Cases - Message IA (Sprint 9-10)
    // -------------------------------------------------------------------------

    @Bean
    public TranscribeVoiceMessageUseCase transcribeVoiceMessageUseCase(
            MessageRepositoryPort messageRepository,
            VoiceTranscriptionPort voiceTranscriptionPort
    ) {
        return new TranscribeVoiceMessageUseCase(messageRepository, voiceTranscriptionPort);
    }

    @Bean
    public TranslateMessageUseCase translateMessageUseCase(
            MessageRepositoryPort messageRepository,
            TranslationPort translationPort
    ) {
        return new TranslateMessageUseCase(messageRepository, translationPort);
    }

    @Bean
    public DetectMessageIntentUseCase detectMessageIntentUseCase(
            MessageRepositoryPort messageRepository,
            IntentDetectionPort intentDetectionPort
    ) {
        return new DetectMessageIntentUseCase(messageRepository, intentDetectionPort);
    }

    @Bean
    public AnalyzeMessageEmotionUseCase analyzeMessageEmotionUseCase(
            MessageRepositoryPort messageRepository,
            EmotionAnalysisPort emotionAnalysisPort
    ) {
        return new AnalyzeMessageEmotionUseCase(messageRepository, emotionAnalysisPort);
    }

    @Bean
    public SummarizeConversationUseCase summarizeConversationUseCase(
            ConversationRepositoryPort conversationRepository,
            MessageRepositoryPort messageRepository,
            ConversationSummaryPort conversationSummaryPort
    ) {
        return new SummarizeConversationUseCase(conversationRepository, messageRepository, conversationSummaryPort);
    }

    @Bean
    public SearchMessagesByIntentUseCase searchMessagesByIntentUseCase(
            ConversationRepositoryPort conversationRepository,
            MessageRepositoryPort messageRepository
    ) {
        return new SearchMessagesByIntentUseCase(conversationRepository, messageRepository);
    }

    // -------------------------------------------------------------------------
    // Use Cases - Social (Cercles de Vie)
    // -------------------------------------------------------------------------

    @Bean
    public CreateCircleUseCase createCircleUseCase(CircleRepositoryPort circleRepository, TimeProvider timeProvider) {
        return new CreateCircleUseCase(circleRepository, timeProvider);
    }

    @Bean
    public AddMemberToCircleUseCase addMemberToCircleUseCase(CircleRepositoryPort circleRepository, TimeProvider timeProvider) {
        return new AddMemberToCircleUseCase(circleRepository, timeProvider);
    }

    @Bean
    public RemoveMemberFromCircleUseCase removeMemberFromCircleUseCase(CircleRepositoryPort circleRepository, TimeProvider timeProvider) {
        return new RemoveMemberFromCircleUseCase(circleRepository, timeProvider);
    }

    @Bean
    public UpdateCircleRulesUseCase updateCircleRulesUseCase(CircleRepositoryPort circleRepository) {
        return new UpdateCircleRulesUseCase(circleRepository);
    }

    @Bean
    public RenameCircleUseCase renameCircleUseCase(CircleRepositoryPort circleRepository) {
        return new RenameCircleUseCase(circleRepository);
    }

    @Bean
    public GetMyCirclesUseCase getMyCirclesUseCase(CircleRepositoryPort circleRepository) {
        return new GetMyCirclesUseCase(circleRepository);
    }

    @Bean
    public SuggestCircleForContactUseCase suggestCircleForContactUseCase(CircleRepositoryPort circleRepository) {
        return new SuggestCircleForContactUseCase(circleRepository);
    }

    // -------------------------------------------------------------------------
    // Use Cases - Workspace
    // -------------------------------------------------------------------------

    @Bean
    public CreateProjectUseCase createProjectUseCase(ProjectRepositoryPort projectRepository, TimeProvider timeProvider) {
        return new CreateProjectUseCase(projectRepository, timeProvider);
    }

    @Bean
    public GetProjectsByMemberUseCase getProjectsByMemberUseCase(ProjectRepositoryPort projectRepository) {
        return new GetProjectsByMemberUseCase(projectRepository);
    }

    @Bean
    public CreateTaskUseCase createTaskUseCase(
            ProjectRepositoryPort projectRepository,
            TaskRepositoryPort taskRepository,
            TimeProvider timeProvider
    ) {
        return new CreateTaskUseCase(projectRepository, taskRepository, timeProvider);
    }

    @Bean
    public UpdateTaskStatusUseCase updateTaskStatusUseCase(TaskRepositoryPort taskRepository, TimeProvider timeProvider) {
        return new UpdateTaskStatusUseCase(taskRepository, timeProvider);
    }

    @Bean
    public GetTasksByStatusUseCase getTasksByStatusUseCase(TaskRepositoryPort taskRepository) {
        return new GetTasksByStatusUseCase(taskRepository);
    }

    @Bean
    public GetTasksByAssigneeUseCase getTasksByAssigneeUseCase(TaskRepositoryPort taskRepository) {
        return new GetTasksByAssigneeUseCase(taskRepository);
    }

    @Bean
    public GetKanbanBoardUseCase getKanbanBoardUseCase(
            ProjectRepositoryPort projectRepository,
            TaskRepositoryPort taskRepository
    ) {
        return new GetKanbanBoardUseCase(projectRepository, taskRepository);
    }

    @Bean
    public TrackTimeUseCase trackTimeUseCase(
            ProjectRepositoryPort projectRepository,
            TimeEntryRepositoryPort timeEntryRepository,
            TimeProvider timeProvider
    ) {
        return new TrackTimeUseCase(projectRepository, timeEntryRepository, timeProvider);
    }

    @Bean
    public GetRunningTimeEntryUseCase getRunningTimeEntryUseCase(TimeEntryRepositoryPort timeEntryRepository) {
        return new GetRunningTimeEntryUseCase(timeEntryRepository);
    }

    @Bean
    public GetTimeReportUseCase getTimeReportUseCase(TimeEntryRepositoryPort timeEntryRepository) {
        return new GetTimeReportUseCase(timeEntryRepository);
    }

    // -------------------------------------------------------------------------
    // Use Cases - Invoice
    // -------------------------------------------------------------------------

    @Bean
    public CreateInvoiceUseCase createInvoiceUseCase(InvoiceRepositoryPort invoiceRepository, TimeProvider timeProvider) {
        return new CreateInvoiceUseCase(invoiceRepository, timeProvider);
    }

    @Bean
    public SendInvoiceUseCase sendInvoiceUseCase(InvoiceRepositoryPort invoiceRepository, TimeProvider timeProvider) {
        return new SendInvoiceUseCase(invoiceRepository, timeProvider);
    }

    @Bean
    public MarkInvoicePaidUseCase markInvoicePaidUseCase(InvoiceRepositoryPort invoiceRepository, TimeProvider timeProvider) {
        return new MarkInvoicePaidUseCase(invoiceRepository, timeProvider);
    }

    @Bean
    public CancelInvoiceUseCase cancelInvoiceUseCase(InvoiceRepositoryPort invoiceRepository, TimeProvider timeProvider) {
        return new CancelInvoiceUseCase(invoiceRepository, timeProvider);
    }

    @Bean
    public GetInvoiceUseCase getInvoiceUseCase(InvoiceRepositoryPort invoiceRepository) {
        return new GetInvoiceUseCase(invoiceRepository);
    }

    // -------------------------------------------------------------------------
    // Use Cases - Assistant IA (Reminders)
    // -------------------------------------------------------------------------

    @Bean
    public CreateReminderUseCase createReminderUseCase(ReminderRepositoryPort reminderRepository, TimeProvider timeProvider) {
        return new CreateReminderUseCase(reminderRepository, timeProvider);
    }

    @Bean
    public TriggerReminderUseCase triggerReminderUseCase(ReminderRepositoryPort reminderRepository, TimeProvider timeProvider) {
        return new TriggerReminderUseCase(reminderRepository, timeProvider);
    }

    @Bean
    public SnoozeReminderUseCase snoozeReminderUseCase(ReminderRepositoryPort reminderRepository, TimeProvider timeProvider) {
        return new SnoozeReminderUseCase(reminderRepository, timeProvider);
    }

    @Bean
    public DismissReminderUseCase dismissReminderUseCase(ReminderRepositoryPort reminderRepository, TimeProvider timeProvider) {
        return new DismissReminderUseCase(reminderRepository, timeProvider);
    }

    @Bean
    public GetRemindersUseCase getRemindersUseCase(ReminderRepositoryPort reminderRepository) {
        return new GetRemindersUseCase(reminderRepository);
    }

    // -------------------------------------------------------------------------
    // Use Cases - Wellbeing
    // -------------------------------------------------------------------------

    @Bean
    public StartFocusSessionUseCase startFocusSessionUseCase(FocusSessionRepositoryPort focusSessionRepository, TimeProvider timeProvider) {
        return new StartFocusSessionUseCase(focusSessionRepository, timeProvider);
    }

    @Bean
    public EndFocusSessionUseCase endFocusSessionUseCase(FocusSessionRepositoryPort focusSessionRepository, TimeProvider timeProvider) {
        return new EndFocusSessionUseCase(focusSessionRepository, timeProvider);
    }

    @Bean
    public InterruptFocusSessionUseCase interruptFocusSessionUseCase(FocusSessionRepositoryPort focusSessionRepository, TimeProvider timeProvider) {
        return new InterruptFocusSessionUseCase(focusSessionRepository, timeProvider);
    }

    @Bean
    public TrackAppUsageUseCase trackAppUsageUseCase(UsageSessionRepositoryPort usageSessionRepository, TimeProvider timeProvider) {
        return new TrackAppUsageUseCase(usageSessionRepository, timeProvider);
    }

    @Bean
    public GetWellbeingReportUseCase getWellbeingReportUseCase(
            FocusSessionRepositoryPort focusSessionRepository,
            UsageSessionRepositoryPort usageSessionRepository
    ) {
        return new GetWellbeingReportUseCase(focusSessionRepository, usageSessionRepository);
    }

    @Bean
    public GetFocusSessionHistoryUseCase getFocusSessionHistoryUseCase(
            FocusSessionRepositoryPort focusSessionRepository
    ) {
        return new GetFocusSessionHistoryUseCase(focusSessionRepository);
    }

    @Bean
    public GetUsageSessionsByAppTypeUseCase getUsageSessionsByAppTypeUseCase(
            UsageSessionRepositoryPort usageSessionRepository
    ) {
        return new GetUsageSessionsByAppTypeUseCase(usageSessionRepository);
    }

    @Bean
    public CreateQrCodeUseCase createQrCodeUseCase(
            QrCodeRepositoryPort qrCodeRepository,
            TimeProvider timeProvider
    ) {
        return new CreateQrCodeUseCase(qrCodeRepository, timeProvider);
    }

    @Bean
    public GetMyQrCodesUseCase getMyQrCodesUseCase(
            QrCodeRepositoryPort qrCodeRepository,
            TimeProvider timeProvider
    ) {
        return new GetMyQrCodesUseCase(qrCodeRepository, timeProvider);
    }

    @Bean
    public ScanQrCodeUseCase scanQrCodeUseCase(
            QrCodeRepositoryPort qrCodeRepository,
            TimeProvider timeProvider,
            GeolocationPort geolocationPort
    ) {
        return new ScanQrCodeUseCase(qrCodeRepository, timeProvider, geolocationPort);
    }

    @Bean
    public RevokeQrCodeUseCase revokeQrCodeUseCase(
            QrCodeRepositoryPort qrCodeRepository,
            TimeProvider timeProvider
    ) {
        return new RevokeQrCodeUseCase(qrCodeRepository, timeProvider);
    }

    @Bean
    public ConfigureQrCodeRulesUseCase configureQrCodeRulesUseCase(QrCodeRepositoryPort qrCodeRepository) {
        return new ConfigureQrCodeRulesUseCase(qrCodeRepository);
    }

    @Bean
    public SuggestQrCodeTypeUseCase suggestQrCodeTypeUseCase(
            GeolocationPort geolocationPort,
            CalendarIntegrationPort calendarIntegrationPort,
            TimeProvider timeProvider
    ) {
        return new SuggestQrCodeTypeUseCase(geolocationPort, calendarIntegrationPort, timeProvider);
    }
}
