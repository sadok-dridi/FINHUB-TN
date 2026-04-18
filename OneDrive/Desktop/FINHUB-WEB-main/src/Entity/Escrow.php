<?php

namespace App\Entity;

use App\Repository\EscrowRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: EscrowRepository::class)]
#[ORM\Table(name: 'escrow')]
class Escrow
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne]
    #[ORM\JoinColumn(name: 'sender_wallet_id', nullable: false)]
    private ?Wallet $senderWallet = null;

    #[ORM\ManyToOne]
    #[ORM\JoinColumn(name: 'receiver_wallet_id', nullable: false)]
    private ?Wallet $receiverWallet = null;

    #[ORM\Column(type: Types::DECIMAL, precision: 15, scale: 3)]
    private ?string $amount = null;

    #[ORM\Column(name: 'condition_text', type: Types::TEXT, nullable: true)]
    private ?string $conditionText = null;

    #[ORM\Column(name: 'escrow_type', length: 20, options: ['default' => 'QR_CODE'])]
    private ?string $escrowType = 'QR_CODE';

    #[ORM\Column(name: 'secret_code', length: 255, nullable: true)]
    private ?string $secretCode = null;

    #[ORM\Column(name: 'qr_code_image', type: Types::TEXT, nullable: true)]
    private ?string $qrCodeImage = null;

    #[ORM\ManyToOne]
    #[ORM\JoinColumn(name: 'admin_approver_id', referencedColumnName: 'user_id', nullable: true)]
    private ?User $adminApprover = null;

    #[ORM\Column(name: 'expiry_date', type: Types::DATETIME_MUTABLE, nullable: true)]
    private ?\DateTimeInterface $expiryDate = null;

    #[ORM\Column(name: 'require_docusign', type: Types::BOOLEAN, options: ['default' => 0])]
    private ?bool $requireDocusign = false;

    #[ORM\Column(name: 'docusign_envelope_id', length: 255, nullable: true)]
    private ?string $docusignEnvelopeId = null;

    #[ORM\Column(name: 'is_disputed', type: Types::BOOLEAN, options: ['default' => 0])]
    private ?bool $isDisputed = false;

    #[ORM\Column(length: 20, options: ['default' => 'LOCKED'])]
    private ?string $status = 'LOCKED';

    #[ORM\Column(name: 'created_at', type: Types::DATETIME_MUTABLE, options: ['default' => 'CURRENT_TIMESTAMP'], nullable: true)]
    private ?\DateTimeInterface $createdAt = null;

    #[ORM\Column(name: 'updated_at', type: Types::DATETIME_MUTABLE, options: ['default' => 'CURRENT_TIMESTAMP'], nullable: true)]
    private ?\DateTimeInterface $updatedAt = null;

    public function __construct()
    {
        $this->createdAt = new \DateTime();
        $this->updatedAt = new \DateTime();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getSenderWallet(): ?Wallet
    {
        return $this->senderWallet;
    }

    public function setSenderWallet(?Wallet $senderWallet): static
    {
        $this->senderWallet = $senderWallet;

        return $this;
    }

    public function getReceiverWallet(): ?Wallet
    {
        return $this->receiverWallet;
    }

    public function setReceiverWallet(?Wallet $receiverWallet): static
    {
        $this->receiverWallet = $receiverWallet;

        return $this;
    }

    public function getAmount(): ?string
    {
        return $this->amount;
    }

    public function setAmount(string $amount): static
    {
        $this->amount = $amount;

        return $this;
    }

    public function getConditionText(): ?string
    {
        return $this->conditionText;
    }

    public function setConditionText(?string $conditionText): static
    {
        $this->conditionText = $conditionText;

        return $this;
    }

    public function getEscrowType(): ?string
    {
        return $this->escrowType;
    }

    public function setEscrowType(?string $escrowType): static
    {
        $this->escrowType = $escrowType;

        return $this;
    }

    public function getSecretCode(): ?string
    {
        return $this->secretCode;
    }

    public function setSecretCode(?string $secretCode): static
    {
        $this->secretCode = $secretCode;

        return $this;
    }

    public function getQrCodeImage(): ?string
    {
        return $this->qrCodeImage;
    }

    public function setQrCodeImage(?string $qrCodeImage): static
    {
        $this->qrCodeImage = $qrCodeImage;

        return $this;
    }

    public function getAdminApprover(): ?User
    {
        return $this->adminApprover;
    }

    public function setAdminApprover(?User $adminApprover): static
    {
        $this->adminApprover = $adminApprover;

        return $this;
    }

    public function getExpiryDate(): ?\DateTimeInterface
    {
        return $this->expiryDate;
    }

    public function setExpiryDate(?\DateTimeInterface $expiryDate): static
    {
        $this->expiryDate = $expiryDate;

        return $this;
    }

    public function isRequireDocusign(): ?bool
    {
        return $this->requireDocusign;
    }

    public function setRequireDocusign(bool $requireDocusign): static
    {
        $this->requireDocusign = $requireDocusign;

        return $this;
    }

    public function getDocusignEnvelopeId(): ?string
    {
        return $this->docusignEnvelopeId;
    }

    public function setDocusignEnvelopeId(?string $docusignEnvelopeId): static
    {
        $this->docusignEnvelopeId = $docusignEnvelopeId;

        return $this;
    }

    public function isDisputed(): ?bool
    {
        return $this->isDisputed;
    }

    public function setIsDisputed(?bool $isDisputed): static
    {
        $this->isDisputed = $isDisputed;

        return $this;
    }

    public function getStatus(): ?string
    {
        return $this->status;
    }

    public function setStatus(?string $status): static
    {
        $this->status = $status;

        return $this;
    }

    public function getCreatedAt(): ?\DateTimeInterface
    {
        return $this->createdAt;
    }

    public function setCreatedAt(?\DateTimeInterface $createdAt): static
    {
        $this->createdAt = $createdAt;

        return $this;
    }

    public function getUpdatedAt(): ?\DateTimeInterface
    {
        return $this->updatedAt;
    }

    public function setUpdatedAt(?\DateTimeInterface $updatedAt): static
    {
        $this->updatedAt = $updatedAt;

        return $this;
    }
}
