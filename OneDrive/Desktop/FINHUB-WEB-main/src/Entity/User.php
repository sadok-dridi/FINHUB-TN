<?php

namespace App\Entity;

use App\Repository\UserRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Security\Core\User\UserInterface;

#[ORM\Entity(repositoryClass: UserRepository::class)]
#[ORM\Table(name: 'users_local')]
class User implements UserInterface
{
    #[ORM\Id]
    #[ORM\Column(name: 'user_id', type: 'integer')]
    #[ORM\GeneratedValue(strategy: 'NONE')]
    private ?int $id = null;

    #[ORM\Column(length: 255, unique: true)]
    private ?string $email = null;

    #[ORM\Column(length: 20)]
    private ?string $role = null;

    #[ORM\Column(length: 255)]
    private ?string $full_name = null;

    #[ORM\Column(length: 20, nullable: true)]
    private ?string $phone_number = null;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $profile_photo_url = null;

    #[ORM\Column(length: 50, nullable: true)]
    private ?string $telegram_chat_id = null;

    #[ORM\Column(options: ['default' => 100])]
    private ?int $trust_score = 100;

    #[ORM\Column(type: 'datetime', options: ['default' => 'CURRENT_TIMESTAMP'])]
    private ?\DateTimeInterface $synced_at = null;

    #[ORM\OneToOne(mappedBy: 'user', cascade: ['persist', 'remove'])]
    private ?Wallet $wallet = null;

    #[ORM\OneToOne(mappedBy: 'user', cascade: ['persist', 'remove'])]
    private ?FinancialProfile $financialProfile = null;

    #[ORM\OneToMany(mappedBy: 'user', targetEntity: SupportTicket::class)]
    private Collection $supportTickets;

    #[ORM\OneToMany(mappedBy: 'user', targetEntity: KycRequest::class, cascade: ['persist', 'remove'])]
    private Collection $kycRequests;

    public function __construct()
    {
        $this->supportTickets = new ArrayCollection();
        $this->kycRequests = new ArrayCollection();
    }
    public function getId(): ?int
    {
        return $this->id;
    }

    // Set ID manually because it comes from API
    public function setId(int $id): static
    {
        $this->id = $id;
        return $this;
    }

    public function getEmail(): ?string
    {
        return $this->email;
    }

    public function setEmail(string $email): static
    {
        $this->email = $email;

        return $this;
    }

    /**
     * A visual identifier that represents this user.
     *
     * @see UserInterface
     */
    public function getUserIdentifier(): string
    {
        return (string) $this->email;
    }

    /**
     * @see UserInterface
     */
    public function getRoles(): array
    {
        $role = strtoupper((string) $this->role);

        return match ($role) {
            'ADMIN', 'ROLE_ADMIN' => ['ROLE_ADMIN'],
            'CLIENT', 'USER', 'ROLE_USER' => ['ROLE_USER'],
            '' => ['ROLE_USER'],
            default => [str_starts_with($role, 'ROLE_') ? $role : 'ROLE_' . $role],
        };
    }

    public function setRoles(array $roles): static
    {
        foreach ($roles as $r) {
            $role = strtoupper((string) $r);

            if ($role === 'ROLE_ADMIN' || $role === 'ADMIN') {
                $this->role = 'ADMIN';
                return $this;
            }

            if ($role === 'ROLE_USER' || $role === 'USER' || $role === 'CLIENT') {
                $this->role = 'USER';
                return $this;
            }
        }

        if (!$this->role && !empty($roles)) {
            $this->role = strtoupper((string) $roles[0]);
        }
        
        return $this;
    }

    public function getRole(): ?string
    {
        return $this->role;
    }

    public function setRole(string $role): static
    {
        $this->role = $role;
        return $this;
    }

    /**
     * @see UserInterface
     */
    public function eraseCredentials(): void
    {
        // If you store any temporary, sensitive data on the user, clear it here
        // $this->plainPassword = null;
    }

    public function getFullName(): ?string
    {
        return $this->full_name;
    }

    public function setFullName(?string $full_name): static
    {
        $this->full_name = $full_name;

        return $this;
    }

    public function getPhoneNumber(): ?string
    {
        return $this->phone_number;
    }

    public function setPhoneNumber(?string $phone_number): static
    {
        $this->phone_number = $phone_number;

        return $this;
    }

    public function getProfilePhotoUrl(): ?string
    {
        return $this->profile_photo_url;
    }

    public function setProfilePhotoUrl(?string $profile_photo_url): static
    {
        $this->profile_photo_url = $profile_photo_url;

        return $this;
    }

    public function getTelegramChatId(): ?string
    {
        return $this->telegram_chat_id;
    }

    public function setTelegramChatId(?string $telegram_chat_id): static
    {
        $this->telegram_chat_id = $telegram_chat_id;

        return $this;
    }

    public function getTrustScore(): ?int
    {
        return $this->trust_score;
    }

    public function setTrustScore(int $trust_score): static
    {
        $this->trust_score = $trust_score;

        return $this;
    }

    public function getSyncedAt(): ?\DateTimeInterface
    {
        return $this->synced_at;
    }

    public function setSyncedAt(\DateTimeInterface $synced_at): static
    {
        $this->synced_at = $synced_at;

        return $this;
    }

    public function getWallet(): ?Wallet
    {
        return $this->wallet;
    }

    public function setWallet(Wallet $wallet): static
    {
        // set the owning side of the relation if necessary
        if ($wallet->getUser() !== $this) {
            $wallet->setUser($this);
        }

        $this->wallet = $wallet;

        return $this;
    }

    public function getFinancialProfile(): ?FinancialProfile
    {
        return $this->financialProfile;
    }

    public function setFinancialProfile(FinancialProfile $financialProfile): static
    {
        // set the owning side of the relation if necessary
        if ($financialProfile->getUser() !== $this) {
            $financialProfile->setUser($this);
        }

        $this->financialProfile = $financialProfile;

        return $this;
    }

    /**
     * @return Collection<int, SupportTicket>
     */
    public function getSupportTickets(): Collection
    {
        return $this->supportTickets;
    }

    public function addSupportTicket(SupportTicket $supportTicket): static
    {
        if (!$this->supportTickets->contains($supportTicket)) {
            $this->supportTickets->add($supportTicket);
            $supportTicket->setUser($this);
        }

        return $this;
    }

    public function removeSupportTicket(SupportTicket $supportTicket): static
    {
        if ($this->supportTickets->removeElement($supportTicket)) {
            // set the owning side to null (unless already changed)
            if ($supportTicket->getUser() === $this) {
                $supportTicket->setUser(null);
            }
        }

        return $this;
    }

    /**
     * @return Collection<int, KycRequest>
     */
    public function getKycRequests(): Collection
    {
        return $this->kycRequests;
    }

    public function addKycRequest(KycRequest $kycRequest): static
    {
        if (!$this->kycRequests->contains($kycRequest)) {
            $this->kycRequests->add($kycRequest);
            $kycRequest->setUser($this);
        }

        return $this;
    }

    public function removeKycRequest(KycRequest $kycRequest): static
    {
        if ($this->kycRequests->removeElement($kycRequest)) {
            // set the owning side to null (unless already changed)
            if ($kycRequest->getUser() === $this) {
                $kycRequest->setUser(null);
            }
        }

        return $this;
    }
}
