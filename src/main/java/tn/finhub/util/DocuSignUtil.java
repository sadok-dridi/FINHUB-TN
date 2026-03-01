package tn.finhub.util;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.*;

import io.github.cdimascio.dotenv.Dotenv;
import tn.finhub.model.Escrow;
import tn.finhub.model.User;
import tn.finhub.model.UserModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class DocuSignUtil {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String CLIENT_ID = dotenv.get("DOCUSIGN_CLIENT_ID");
    private static final String IMPERSONATED_USER_ID = dotenv.get("DOCUSIGN_USER_ID");
    private static final String PRIVATE_KEY_PATH = dotenv.get("DOCUSIGN_PRIVATE_KEY_PATH");
    private static final String BASE_PATH = dotenv.get("DOCUSIGN_BASE_PATH");

    // We request full signature scope plus impersonation
    private static final List<String> SCOPES = Arrays.asList("signature", "impersonation");

    private static ApiClient getApiClient() throws ApiException, IOException {
        ApiClient apiClient = new ApiClient(BASE_PATH);
        apiClient.setOAuthBasePath("account-d.docusign.com");

        byte[] privateKeyBytes = Files.readAllBytes(Paths.get(PRIVATE_KEY_PATH));

        OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(
                CLIENT_ID,
                IMPERSONATED_USER_ID,
                SCOPES,
                privateKeyBytes,
                3600);

        apiClient.setAccessToken(oAuthToken.getAccessToken(), oAuthToken.getExpiresIn());
        return apiClient;
    }

    public static String createEscrowAgreementEnvelope(Escrow escrow, String accountId)
            throws ApiException, IOException {
        ApiClient apiClient = getApiClient();
        EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);

        // Fetch user info for sender and receiver to get emails/names
        UserModel userModel = new UserModel();
        User sender = userModel.findById(getUserIdFromWalletOrElse(escrow.getSenderWalletId(), 1)); // Ideally query
                                                                                                    // properly
        User receiver = userModel.findById(getUserIdFromWalletOrElse(escrow.getReceiverWalletId(), 2));

        EnvelopeDefinition envelope = new EnvelopeDefinition();
        envelope.setEmailSubject("FINHUB-TN: Escrow Agreement Signature Required");

        // Document
        Document doc = new Document();
        String htmlContent = buildAgreementHtml(escrow, sender, receiver);
        String base64Content = Base64.getEncoder().encodeToString(htmlContent.getBytes());
        doc.setDocumentBase64(base64Content);
        doc.setName("Escrow Agreement");
        doc.setFileExtension("html");
        doc.setDocumentId("1");

        envelope.setDocuments(Collections.singletonList(doc));

        // Signers
        Signer signer1 = createSigner(sender, "1", "1");
        Signer signer2 = createSigner(receiver, "2", "2");

        Recipients recipients = new Recipients();
        recipients.setSigners(Arrays.asList(signer1, signer2));
        envelope.setRecipients(recipients);

        envelope.setStatus("sent");

        EnvelopeSummary results = envelopesApi.createEnvelope(accountId, envelope);
        return results.getEnvelopeId();
    }

    private static int getUserIdFromWalletOrElse(int walletId, int fallback) {
        try {
            tn.finhub.model.WalletModel walletModel = new tn.finhub.model.WalletModel();
            tn.finhub.model.Wallet wallet = walletModel.findById(walletId);
            if (wallet != null) {
                return wallet.getUserId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fallback;
    }

    private static String buildAgreementHtml(Escrow escrow, User sender, User receiver) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>FINHUB-TN Escrow Agreement</title>" +
                "<style>" +
                "  body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333; line-height: 1.6; padding: 40px; }"
                +
                "  .container { max-width: 800px; margin: 0 auto; border: 1px solid #e0e0e0; padding: 40px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }"
                +
                "  .header { border-bottom: 2px solid #5a55cc; padding-bottom: 20px; margin-bottom: 30px; }" +
                "  .logo { font-size: 24px; font-weight: bold; color: #5a55cc; }" +
                "  .title { font-size: 28px; color: #1a1a1a; margin-top: 10px; }" +
                "  .section { margin-bottom: 25px; }" +
                "  .section-title { font-size: 18px; font-weight: bold; color: #5a55cc; border-bottom: 1px solid #eee; padding-bottom: 8px; margin-bottom: 15px; }"
                +
                "  .detail-row { margin-bottom: 10px; display: table; width: 100%; }" +
                "  .detail-label { display: table-cell; width: 150px; font-weight: bold; color: #666; }" +
                "  .detail-value { display: table-cell; }" +
                "  .amount-box { background-color: #f8f9fa; border-left: 4px solid #5a55cc; padding: 15px; font-size: 18px; margin: 20px 0; }"
                +
                "  .signatures { margin-top: 60px; display: table; width: 100%; }" +
                "  .signature-block { display: table-cell; width: 50%; padding-right: 20px; }" +
                "  .signature-line { border-bottom: 1px solid #333; margin-top: 40px; margin-bottom: 5px; height: 40px; position: relative; }"
                +
                "  .signature-anchor { position: absolute; color: white; bottom: 5px; left: 10px; font-size: 1px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"container\">" +
                "    <div class=\"header\">" +
                "      <div class=\"logo\">FINHUB-TN</div>" +
                "      <div class=\"title\">Digital Escrow Agreement</div>" +
                "    </div>" +
                "    " +
                "    <div class=\"section\">" +
                "      <div class=\"section-title\">Transaction Details</div>" +
                "      <div class=\"amount-box\">" +
                "        <strong>Escrow Amount:</strong> " + escrow.getAmount() + " TND" +
                "      </div>" +
                "      <div class=\"detail-row\">" +
                "        <span class=\"detail-label\">Release Condition:</span>" +
                "        <span class=\"detail-value\">" + escrow.getConditionText() + "</span>" +
                "      </div>" +
                "      <div class=\"detail-row\">" +
                "        <span class=\"detail-label\">Creation Date:</span>" +
                "        <span class=\"detail-value\">" + java.time.LocalDate.now().toString() + "</span>" +
                "      </div>" +
                "    </div>" +
                "    " +
                "    <div class=\"section\">" +
                "      <div class=\"section-title\">Parties Involved</div>" +
                "      <div class=\"detail-row\">" +
                "        <span class=\"detail-label\">Sender (Funder):</span>" +
                "        <span class=\"detail-value\">" + sender.getFullName() + " (" + sender.getEmail() + ")</span>" +
                "      </div>" +
                "      <div class=\"detail-row\">" +
                "        <span class=\"detail-label\">Receiver (Beneficiary):</span>" +
                "        <span class=\"detail-value\">" + receiver.getFullName() + " (" + receiver.getEmail()
                + ")</span>" +
                "      </div>" +
                "    </div>" +
                "    " +
                "    <p style=\"margin-top: 30px; font-size: 14px; color: #666; text-align: justify;\">" +
                "      This digital agreement binds both parties to the terms of the FINHUB-TN Escrow Service. " +
                "      The funds described above have been securely locked in the sender's vault. They will only be released "
                +
                "      to the receiver once both parties have cryptographically signed this document and the release condition "
                +
                "      has been satisfied. By signing below, you agree to these terms." +
                "    </p>" +
                "    " +
                "    <div class=\"signatures\">" +
                "      <div class=\"signature-block\">" +
                "        <strong>Sender Signature</strong><br/>" +
                "        <span style=\"color:#888; font-size:12px;\">" + sender.getFullName() + "</span>" +
                "        <div class=\"signature-line\">" +
                "          <span class=\"signature-anchor\">/sn1/</span>" +
                "        </div>" +
                "      </div>" +
                "      <div class=\"signature-block\">" +
                "        <strong>Receiver Signature</strong><br/>" +
                "        <span style=\"color:#888; font-size:12px;\">" + receiver.getFullName() + "</span>" +
                "        <div class=\"signature-line\">" +
                "          <span class=\"signature-anchor\">/sn2/</span>" +
                "        </div>" +
                "      </div>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    private static Signer createSigner(User user, String recipientId, String tabLabel) {
        Signer signer = new Signer();
        signer.setEmail(user.getEmail());
        signer.setName(user.getFullName());
        signer.setRecipientId(recipientId);
        signer.setRoutingOrder(recipientId);

        SignHere signHere = new SignHere();
        signHere.setAnchorString("/sn" + tabLabel + "/");
        signHere.setAnchorUnits("pixels");
        signHere.setAnchorYOffset("10");
        signHere.setAnchorXOffset("20");

        Tabs signerTabs = new Tabs();
        signerTabs.setSignHereTabs(Collections.singletonList(signHere));
        signer.setTabs(signerTabs);

        return signer;
    }

    public static String getEnvelopeStatus(String envelopeId, String accountId) throws ApiException, IOException {
        ApiClient apiClient = getApiClient();
        EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
        Envelope env = envelopesApi.getEnvelope(accountId, envelopeId);
        return env.getStatus();
    }
}
