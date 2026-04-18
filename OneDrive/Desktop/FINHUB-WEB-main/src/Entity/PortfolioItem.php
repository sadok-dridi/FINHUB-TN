<?php

namespace App\Entity;

use App\Repository\PortfolioItemRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: PortfolioItemRepository::class)]
#[ORM\Table(name: 'portfolio_items')]
class PortfolioItem
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', nullable: false)]
    private ?User $user = null;

    #[ORM\Column(length: 20)]
    private ?string $symbol = null;

    #[ORM\Column(type: Types::DECIMAL, precision: 18, scale: 8)]
    private ?string $quantity = null;

    #[ORM\Column(name: 'average_cost', type: Types::DECIMAL, precision: 18, scale: 8)]
    private ?string $averageCost = null;

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

    public function getSymbol(): ?string
    {
        return $this->symbol;
    }

    public function setSymbol(string $symbol): static
    {
        // Always store symbols in uppercase
        $this->symbol = strtoupper($symbol);

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

    public function getAverageCost(): ?string
    {
        return $this->averageCost;
    }

    public function setAverageCost(string $averageCost): static
    {
        $this->averageCost = $averageCost;

        return $this;
    }
}
