<?php

namespace App\Entity;

use App\Repository\FinancialProfileRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: FinancialProfileRepository::class)]
#[ORM\Table(name: 'financial_profiles_local')]
#[ORM\HasLifecycleCallbacks]
class FinancialProfile
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\OneToOne(inversedBy: 'financialProfile', cascade: ['persist', 'remove'])]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', nullable: false)]
    private ?User $user = null;

    #[ORM\Column(nullable: true)]
    #[Assert\NotBlank(message: "Monthly income is required")]
    #[Assert\PositiveOrZero(message: "Income cannot be negative")]
    private ?float $monthly_income = null;

    #[ORM\Column(nullable: true)]
    #[Assert\NotBlank(message: "Monthly expenses are required")]
    #[Assert\PositiveOrZero(message: "Expenses cannot be negative")]
    private ?float $monthly_expenses = null;

    #[ORM\Column(nullable: true)]
    #[Assert\NotBlank(message: "Savings goal is required")]
    #[Assert\PositiveOrZero(message: "Savings goal cannot be negative")]
    private ?float $savings_goal = null;

    #[ORM\Column(length: 10, nullable: true)]
    #[Assert\NotBlank(message: "Risk tolerance must be selected")]
    #[Assert\Choice(choices: ["LOW", "MEDIUM", "HIGH"], message: "Invalid risk tolerance")]
    private ?string $risk_tolerance = null;

    #[ORM\Column(length: 5, options: ['default' => 'TND'])]
    private ?string $currency = 'TND';

    #[ORM\Column(type: Types::BOOLEAN, options: ['default' => 0])]
    private ?bool $profile_completed = false;

    #[ORM\Column(type: 'datetime', options: ['default' => 'CURRENT_TIMESTAMP'])]
    private ?\DateTimeInterface $created_at = null;

    #[ORM\Column(type: 'datetime', options: ['default' => 'CURRENT_TIMESTAMP'])]
    private ?\DateTimeInterface $updated_at = null;

    public function __construct()
    {
        $this->created_at = new \DateTime();
        $this->updated_at = new \DateTime();
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

    public function getMonthlyIncome(): ?float
    {
        return $this->monthly_income;
    }

    public function setMonthlyIncome(?float $monthly_income): static
    {
        $this->monthly_income = $monthly_income;

        return $this;
    }

    public function getMonthlyExpenses(): ?float
    {
        return $this->monthly_expenses;
    }

    public function setMonthlyExpenses(?float $monthly_expenses): static
    {
        $this->monthly_expenses = $monthly_expenses;

        return $this;
    }

    public function getSavingsGoal(): ?float
    {
        return $this->savings_goal;
    }

    public function setSavingsGoal(?float $savings_goal): static
    {
        $this->savings_goal = $savings_goal;

        return $this;
    }

    public function getRiskTolerance(): ?string
    {
        return $this->risk_tolerance;
    }

    public function setRiskTolerance(?string $risk_tolerance): static
    {
        $this->risk_tolerance = $risk_tolerance;

        return $this;
    }

    public function getCurrency(): ?string
    {
        return $this->currency;
    }

    public function setCurrency(string $currency): static
    {
        $this->currency = $currency;

        return $this;
    }

    public function isProfileCompleted(): ?bool
    {
        return $this->profile_completed;
    }

    public function setProfileCompleted(bool $profile_completed): static
    {
        $this->profile_completed = $profile_completed;

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

    public function getUpdatedAt(): ?\DateTimeInterface
    {
        return $this->updated_at;
    }

    public function setUpdatedAt(\DateTimeInterface $updated_at): static
    {
        $this->updated_at = $updated_at;

        return $this;
    }

    #[ORM\PreUpdate]
    public function preUpdate(): void
    {
        $this->updated_at = new \DateTime('now', new \DateTimeZone('Africa/Tunis'));
    }
}
