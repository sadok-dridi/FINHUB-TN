<?php

namespace App\Entity;

use App\Repository\SimulatedTradeRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: SimulatedTradeRepository::class)]
#[ORM\Table(name: 'simulated_trades')]
class SimulatedTrade
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', nullable: false)]
    private ?User $user = null;

    #[ORM\Column(name: 'asset_symbol', length: 20)]
    private ?string $assetSymbol = null;

    #[ORM\Column(name: 'action', length: 10)] // e.g., 'BUY' or 'SELL'
    private ?string $action = null;

    #[ORM\Column(type: Types::DECIMAL, precision: 12, scale: 4)]
    private ?string $quantity = null;

    #[ORM\Column(name: 'price_at_transaction', type: Types::DECIMAL, precision: 12, scale: 4)]
    private ?string $priceAtTransaction = null;

    #[ORM\Column(name: 'total_cost', type: Types::DECIMAL, precision: 12, scale: 4, nullable: true, insertable: false, updatable: false)]
    private ?string $totalCost = null;

    #[ORM\Column(name: 'transaction_date', type: Types::DATETIME_MUTABLE)]
    private ?\DateTimeInterface $transactionDate = null;

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

    public function getAssetSymbol(): ?string
    {
        return $this->assetSymbol;
    }

    public function setAssetSymbol(string $assetSymbol): static
    {
        $this->assetSymbol = $assetSymbol;

        return $this;
    }

    public function getAction(): ?string
    {
        return $this->action;
    }

    public function setAction(string $action): static
    {
        $this->action = $action;

        return $this;
    }

    public function getQuantity(): ?string
    {
        return $this->quantity;
    }

    public function setQuantity(string $quantity): static
    {
        $this->quantity = $quantity;

        return $this;
    }

    public function getPriceAtTransaction(): ?string
    {
        return $this->priceAtTransaction;
    }

    public function setPriceAtTransaction(string $priceAtTransaction): static
    {
        $this->priceAtTransaction = $priceAtTransaction;

        return $this;
    }

    public function getTotalCost(): ?string
    {
        return $this->totalCost;
    }

    public function setTotalCost(?string $totalCost): static
    {
        $this->totalCost = $totalCost;

        return $this;
    }

    public function getTransactionDate(): ?\DateTimeInterface
    {
        return $this->transactionDate;
    }

    public function setTransactionDate(\DateTimeInterface $transactionDate): static
    {
        $this->transactionDate = $transactionDate;

        return $this;
    }
}