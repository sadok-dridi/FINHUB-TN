<?php

namespace App\Form;

use App\Entity\FinancialProfile;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\NotBlank;

class FinancialProfileType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('full_name', TextType::class, [
                'mapped' => false,
                'label' => 'Full Name',
                'required' => true,
                'constraints' => [
                    new NotBlank(['message' => 'Full name is required']),
                    new Length([
                        'min' => 2,
                        'max' => 255,
                        'minMessage' => 'Full name must be at least {{ limit }} characters.',
                    ]),
                ],
            ])
            ->add('monthly_income', NumberType::class, [
                'label' => 'Monthly Income'
            ])
            ->add('monthly_expenses', NumberType::class, [
                'label' => 'Monthly Expenses'
            ])
            ->add('savings_goal', NumberType::class, [
                'label' => 'Savings Goal'
            ])
            ->add('risk_tolerance', ChoiceType::class, [
                'label' => 'Risk Tolerance',
                'choices'  => [
                    'Low' => 'LOW',
                    'Medium' => 'MEDIUM',
                    'High' => 'HIGH',
                ],
            ])
            ->add('phone_number', TextType::class, [
                'mapped' => false,
                'label' => 'Phone Number',
                'required' => true,
                'constraints' => [
                    new NotBlank(['message' => 'Phone number is required']),
                ],
            ])
            ->add('save', SubmitType::class, [
                'label' => 'Complete Profile'
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => FinancialProfile::class,
        ]);
    }
}
