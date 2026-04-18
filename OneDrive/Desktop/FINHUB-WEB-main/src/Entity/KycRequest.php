<?php

namespace App\Entity;

use App\Repository\KycRequestRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: KycRequestRepository::class)]
#[ORM\Table(name: 'kyc_requests')]
class KycRequest
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'request_id')]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: User::class, inversedBy: 'kycRequests')]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', nullable: false, onDelete: "CASCADE")]
    private ?User $user = null;

    #[ORM\Column(name: 'document_type', length: 50)]
    #[Assert\NotBlank(message: "Document type is required")]
    #[Assert\Choice(choices: ["PASSPORT", "ID_CARD", "DRIVERS_LICENSE"], message: "Invalid document type")]
    private ?string $documentType = null;

    #[ORM\Column(name: 'document_url', length: 255)]
    #[Assert\NotBlank(message: "Document URL is required")]
    private ?string $documentUrl = null;

    #[ORM\Column(length: 20)]
    private ?string $status = 'PENDING';

    #[ORM\Column(name: 'submission_date', type: Types::DATETIME_MUTABLE, options: ['default' => 'CURRENT_TIMESTAMP'])]
    private ?\DateTimeInterface $submissionDate = null;

    public function __construct()
    {
        $this->submissionDate = new \DateTime('now', new \DateTimeZone('Africa/Tunis'));
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getUser(): ?User
    {
        return $this->user;
    }

    public function setUser(?User $user): static
    {
        $this->user = $user;
        return $this;
    }

    public function getDocumentType(): ?string
    {
        return $this->documentType;
    }

    public function setDocumentType(string $documentType): static
    {
        $this->documentType = $documentType;
        return $this;
    }

    public function getDocumentUrl(): ?string
    {
        return $this->documentUrl;
    }

    public function setDocumentUrl(string $documentUrl): static
    {
        $this->documentUrl = $documentUrl;
        return $this;
    }

    public function getStatus(): ?string
    {
        return $this->status;
    }

    public function setStatus(string $status): static
    {
        $this->status = $status;
        return $this;
    }

    public function getSubmissionDate(): ?\DateTimeInterface
    {
        return $this->submissionDate;
    }

    public function setSubmissionDate(\DateTimeInterface $submissionDate): static
    {
        $this->submissionDate = $submissionDate;
        return $this;
    }
}
