<?php

namespace App\Entity;

use App\Repository\ExpenseItemRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: ExpenseItemRepository::class)]
#[ORM\Table(name: 'expense_items')]
class ExpenseItem
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Expense::class)]
    #[ORM\JoinColumn(nullable: false, name: "expense_id", referencedColumnName: "id", onDelete: "CASCADE")]
    private ?Expense $expense = null;

    #[ORM\Column(length: 255)]
    private ?string $item_name = null;

    #[ORM\Column(type: Types::DECIMAL, precision: 10, scale: 3)]
    private ?string $price = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getExpense(): ?Expense
    {
        return $this->expense;
    }

    public function setExpense(?Expense $expense): static
    {
        $this->expense = $expense;

        return $this;
    }

    public function getItemName(): ?string
    {
        return $this->item_name;
    }

    public function setItemName(string $item_name): static
    {
        $this->item_name = $item_name;

        return $this;
    }

    public function getPrice(): ?string
    {
        return $this->price;
    }

    public function setPrice(string $price): static
    {
        $this->price = $price;

        return $this;
    }
}
