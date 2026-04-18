<?php

namespace App\Entity;

use App\Repository\SupportMessageRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: SupportMessageRepository::class)]
#[ORM\Table(name: 'support_messages')]
class SupportMessage
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne(inversedBy: 'messages')]
    #[ORM\JoinColumn(name: 'ticket_id', nullable: false)]
    private ?SupportTicket $ticket = null;

    #[ORM\Column(length: 20, nullable: true)]
    private ?string $sender_role = null;

    #[ORM\Column(type: Types::TEXT)]
    private ?string $message = null;

    #[ORM\Column(type: 'datetime', options: ['default' => 'CURRENT_TIMESTAMP'])]
    private ?\DateTimeInterface $created_at = null;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $attachment_path = null;

    public function __construct()
    {
        $this->created_at = new \DateTime();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getTicket(): ?SupportTicket
    {
        return $this->ticket;
    }

    public function setTicket(?SupportTicket $ticket): static
    {
        $this->ticket = $ticket;

        return $this;
    }

    public function getSenderRole(): ?string
    {
        return $this->sender_role;
    }

    public function setSenderRole(?string $sender_role): static
    {
        $this->sender_role = $sender_role;

        return $this;
    }

    public function getMessage(): ?string
    {
        return $this->message;
    }

    public function setMessage(string $message): static
    {
        $this->message = $message;

        return $this;
    }

    public function getCreatedAt(): ?\DateTimeInterface
    {
        return $this->created_at;
    }

    public function setCreatedAt(\DateTimeInterface $created_at): static
    {
        $this->created_at = $created_at;

        return $this;
    }

    public function getAttachmentPath(): ?string
    {
        return $this->attachment_path;
    }

    public function setAttachmentPath(?string $attachment_path): static
    {
        $this->attachment_path = $attachment_path;

        return $this;
    }
}
