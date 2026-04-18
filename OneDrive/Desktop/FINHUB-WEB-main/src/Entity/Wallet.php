<?php

namespace App\Entity;

use App\Repository\WalletRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: WalletRepository::class)]
#[ORM\Table(name: 'wallets')]
class Wallet
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\OneToOne(inversedBy: 'wallet', cascade: ['persist', 'remove'])]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', nullable: false)]
    private ?User $user = null;

    #[ORM\Column(length: 10, options: ['default' => 'TND'])]
    private ?string $currency = 'TND';

    #[ORM\Column(type: Types::DECIMAL, precision: 14, scale: 2, options: ['default' => '0.00'])]
    private ?string $balance = '0.00';

    #[ORM\Column(name: 'escrow_balance', type: Types::DECIMAL, precision: 14, scale: 2, options: ['default' => '0.00'])]
    private ?string $escrowBalance = '0.00';

    #[ORM\Column(length: 20, options: ['default' => 'ACTIVE'])]
    private ?string $status = 'ACTIVE';

    #[ORM\Column(type: 'datetime', options: ['default' => 'CURRENT_TIMESTAMP'])]
    private ?\DateTimeInterface $created_at = null;

    #[ORM\OneToMany(mappedBy: 'wallet', targetEntity: WalletTransaction::class)]
    #[ORM\OrderBy(['created_at' => 'DESC'])]
    private Collection $transactions;

    #[ORM\OneToMany(mappedBy: 'wallet', targetEntity: VirtualCard::class)]
    private Collection $virtualCards;

    public function __construct()
    {
        $this->created_at = new \DateTime();
        $this->transactions = new ArrayCollection();
        $this->virtualCards = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getUser(): ?User
    {
        return $this->user;
    }

    public function setUser(User $user): static
    {
        $this->user = $user;

        return $this;
    }

    public function getCurrency(): ?string
    {
        return $this->currency;
    }

    public function setCurrency(?string $currency): static
    {
        $this->currency = $currency;

        return $this;
    }

    public function getBalance(): ?string
    {
        return $this->balance;
    }

    public function setBalance(string $balance): static
    {
        $this->balance = $balance;

        return $this;
    }

    public function getEscrowBalance(): ?string
    {
        return $this->escrowBalance;
    }

    public function setEscrowBalance(string $escrowBalance): static
    {
        $this->escrowBalance = $escrowBalance;

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
        return $this->created_at;
    }

    public function setCreatedAt(\DateTimeInterface $created_at): static
    {
        $this->created_at = $created_at;

        return $this;
    }

    /**
     * @return Collection<int, WalletTransaction>
     */
    public function getTransactions(): Collection
    {
        return $this->transactions;
    }

    public function addTransaction(WalletTransaction $transaction): static
    {
        if (!$this->transactions->contains($transaction)) {
            $this->transactions->add($transaction);
            $transaction->setWallet($this);
        }

        return $this;
    }

    public function removeTransaction(WalletTransaction $transaction): static
    {
        if ($this->transactions->removeElement($transaction)) {
            // set the owning side to null (unless already changed)
            if ($transaction->getWallet() === $this) {
                $transaction->setWallet(null);
            }
        }

        return $this;
    }

    /**
     * @return Collection<int, VirtualCard>
     */
    public function getVirtualCards(): Collection
    {
        return $this->virtualCards;
    }

    public function addVirtualCard(VirtualCard $virtualCard): static
    {
        if (!$this->virtualCards->contains($virtualCard)) {
            $this->virtualCards->add($virtualCard);
            $virtualCard->setWallet($this);
        }

        return $this;
    }

    public function removeVirtualCard(VirtualCard $virtualCard): static
    {
        if ($this->virtualCards->removeElement($virtualCard)) {
            // set the owning side to null (unless already changed)
            if ($virtualCard->getWallet() === $this) {
                $virtualCard->setWallet(null);
            }
        }

        return $this;
    }
}
