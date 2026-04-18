<?php

namespace App\Service;

use App\Entity\Escrow;
use App\Entity\User;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Lcobucci\JWT\Configuration;
use Lcobucci\JWT\Signer\Rsa\Sha256;
use Lcobucci\JWT\Signer\Key\InMemory;

class DocuSignService
{
    private $httpClient;
    private $clientId;
    private $userId;
    private $accountId;
    private $basePath;
    private $privateKeyPath;

    public function __construct(HttpClientInterface $httpClient, string $docusignClientId, string $docusignUserId, string $docusignApiAccountId, string $docusignBasePath, string $docusignPrivateKeyPath, string $projectDir)
    {
        $this->httpClient = $httpClient;
        $this->clientId = $docusignClientId;
        $this->userId = $docusignUserId;
        $this->accountId = $docusignApiAccountId;
        $this->basePath = $docusignBasePath;
        $this->privateKeyPath = $projectDir . '/' . $docusignPrivateKeyPath;
    }

    private function getAccessToken(): string
    {
        $privateKey = file_get_contents($this->privateKeyPath);
        
        $header = base64_encode(json_encode(['typ' => 'JWT', 'alg' => 'RS256']));
        $payload = base64_encode(json_encode([
            'iss' => $this->clientId,
            'sub' => $this->userId,
            'aud' => 'account-d.docusign.com',
            'iat' => time(),
            'exp' => time() + 3600,
            'scope' => 'signature impersonation'
        ]));
        
        $data = str_replace(['+', '/', '='], ['-', '_', ''], $header . '.' . $payload);
        
        openssl_sign($data, $signature, $privateKey, OPENSSL_ALGO_SHA256);
        $signature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));
        
        $jwt = $data . '.' . $signature;

        $response = $this->httpClient->request('POST', 'https://account-d.docusign.com/oauth/token', [
            'body' => [
                'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
                'assertion' => $jwt
            ],
            'headers' => [
                'Accept' => 'application/json'
            ]
        ]);

        $data = $response->toArray();
        return $data['access_token'];
    }

    public function createEscrowAgreementEnvelope(Escrow $escrow, User $sender, User $receiver): string
    {
        $token = $this->getAccessToken();
        $htmlContent = $this->buildAgreementHtml($escrow, $sender, $receiver);
        
        $payload = [
            'emailSubject' => 'FINHUB-TN: Escrow Agreement Signature Required',
            'documents' => [
                [
                    'documentBase64' => base64_encode($htmlContent),
                    'name' => 'Escrow Agreement',
                    'fileExtension' => 'html',
                    'documentId' => '1'
                ]
            ],
            'recipients' => [
                'signers' => [
                    $this->createSigner($sender, '1', '1'),
                    $this->createSigner($receiver, '2', '2')
                ]
            ],
            'status' => 'sent'
        ];

        $response = $this->httpClient->request('POST', $this->basePath . '/v2.1/accounts/' . $this->accountId . '/envelopes', [
            'headers' => [
                'Authorization' => 'Bearer ' . $token,
                'Content-Type' => 'application/json'
            ],
            'json' => $payload
        ]);

        $data = $response->toArray();
        return $data['envelopeId'];
    }

    private function createSigner(User $user, string $recipientId, string $tabLabel): array
    {
        return [
            'email' => $user->getEmail(),
            'name' => $user->getFullName(),
            'recipientId' => $recipientId,
            'routingOrder' => $recipientId,
            'tabs' => [
                'signHereTabs' => [
                    [
                        'anchorString' => '/sn' . $tabLabel . '/',
                        'anchorUnits' => 'pixels',
                        'anchorYOffset' => '10',
                        'anchorXOffset' => '20'
                    ]
                ]
            ]
        ];
    }

    private function buildAgreementHtml(Escrow $escrow, User $sender, User $receiver): string
    {
        return "<!DOCTYPE html>
                <html>
                <head>
                <meta charset=\"UTF-8\">
                <title>FINHUB-TN Escrow Agreement</title>
                <style>
                  body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333; line-height: 1.6; padding: 40px; }
                  .container { max-width: 800px; margin: 0 auto; border: 1px solid #e0e0e0; padding: 40px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
                  .header { border-bottom: 2px solid #5a55cc; padding-bottom: 20px; margin-bottom: 30px; }
                  .logo { font-size: 24px; font-weight: bold; color: #5a55cc; }
                  .title { font-size: 28px; color: #1a1a1a; margin-top: 10px; }
                  .section { margin-bottom: 25px; }
                  .section-title { font-size: 18px; font-weight: bold; color: #5a55cc; border-bottom: 1px solid #eee; padding-bottom: 8px; margin-bottom: 15px; }
                  .detail-row { margin-bottom: 10px; display: table; width: 100%; }
                  .detail-label { display: table-cell; width: 150px; font-weight: bold; color: #666; }
                  .detail-value { display: table-cell; }
                  .amount-box { background-color: #f8f9fa; border-left: 4px solid #5a55cc; padding: 15px; font-size: 18px; margin: 20px 0; }
                  .signatures { margin-top: 60px; display: table; width: 100%; }
                  .signature-block { display: table-cell; width: 50%; padding-right: 20px; }
                  .signature-line { border-bottom: 1px solid #333; margin-top: 40px; margin-bottom: 5px; height: 40px; position: relative; }
                  .signature-anchor { position: absolute; color: white; bottom: 5px; left: 10px; font-size: 1px; }
                </style>
                </head>
                <body>
                  <div class=\"container\">
                    <div class=\"header\">
                      <div class=\"logo\">FINHUB-TN</div>
                      <div class=\"title\">Digital Escrow Agreement</div>
                    </div>
                    
                    <div class=\"section\">
                      <div class=\"section-title\">Transaction Details</div>
                      <div class=\"amount-box\">
                        <strong>Escrow Amount:</strong> " . $escrow->getAmount() . " TND
                      </div>
                      <div class=\"detail-row\">
                        <span class=\"detail-label\">Release Condition:</span>
                        <span class=\"detail-value\">" . $escrow->getConditionText() . "</span>
                      </div>
                      <div class=\"detail-row\">
                        <span class=\"detail-label\">Creation Date:</span>
                        <span class=\"detail-value\">" . date('Y-m-d') . "</span>
                      </div>
                    </div>
                    
                    <div class=\"section\">
                      <div class=\"section-title\">Parties Involved</div>
                      <div class=\"detail-row\">
                        <span class=\"detail-label\">Sender (Funder):</span>
                        <span class=\"detail-value\">" . $sender->getFullName() . " (" . $sender->getEmail() . ")</span>
                      </div>
                      <div class=\"detail-row\">
                        <span class=\"detail-label\">Receiver (Beneficiary):</span>
                        <span class=\"detail-value\">" . $receiver->getFullName() . " (" . $receiver->getEmail() . ")</span>
                      </div>
                    </div>
                    
                    <p style=\"margin-top: 30px; font-size: 14px; color: #666; text-align: justify;\">
                      This digital agreement binds both parties to the terms of the FINHUB-TN Escrow Service. 
                      The funds described above have been securely locked in the sender's vault. They will only be released 
                      to the receiver once both parties have cryptographically signed this document and the release condition 
                      has been satisfied. By signing below, you agree to these terms.
                    </p>
                    
                    <div class=\"signatures\">
                      <div class=\"signature-block\">
                        <strong>Sender Signature</strong><br/>
                        <span style=\"color:#888; font-size:12px;\">" . $sender->getFullName() . "</span>
                        <div class=\"signature-line\">
                          <span class=\"signature-anchor\">/sn1/</span>
                        </div>
                      </div>
                      <div class=\"signature-block\">
                        <strong>Receiver Signature</strong><br/>
                        <span style=\"color:#888; font-size:12px;\">" . $receiver->getFullName() . "</span>
                        <div class=\"signature-line\">
                          <span class=\"signature-anchor\">/sn2/</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>";
    }
}
