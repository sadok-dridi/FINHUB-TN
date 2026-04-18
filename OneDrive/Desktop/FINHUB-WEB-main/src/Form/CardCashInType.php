<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\GreaterThanOrEqual;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Regex;

class CardCashInType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('amount', NumberType::class, [
                'label' => 'Amount',
                'scale' => 3,
                'constraints' => [
                    new NotBlank(['message' => 'Amount is required']),
                    new GreaterThanOrEqual([
                        'value' => 1,
                        'message' => 'Cash in amount must be at least 1 TND',
                    ]),
                ],
            ])
            ->add('cardNumber', TextType::class, [
                'label' => 'Card Number',
                'constraints' => [
                    new NotBlank(['message' => 'Card number is required']),
                    new Regex([
                        'pattern' => '/^[0-9 ]{12,23}$/',
                        'message' => 'Card number can only contain digits and spaces.',
                    ]),
                ],
                'attr' => [
                    'inputmode' => 'numeric',
                    'autocomplete' => 'cc-number',
                    'placeholder' => '1234 5678 9012 3456',
                ],
            ])
            ->add('expiryDate', TextType::class, [
                'label' => 'Expiration Date',
                'constraints' => [
                    new NotBlank(['message' => 'Expiration date is required']),
                    new Regex([
                        'pattern' => '/^(0[1-9]|1[0-2])\/[0-9]{2}$/',
                        'message' => 'Use MM/YY format.',
                    ]),
                ],
                'attr' => [
                    'inputmode' => 'numeric',
                    'autocomplete' => 'cc-exp',
                    'placeholder' => 'MM/YY',
                ],
            ])
            ->add('cvv', TextType::class, [
                'label' => 'CVV',
                'constraints' => [
                    new NotBlank(['message' => 'CVV is required']),
                    new Length(['min' => 3, 'max' => 4]),
                    new Regex([
                        'pattern' => '/^[0-9]{3,4}$/',
                        'message' => 'CVV must contain 3 or 4 digits.',
                    ]),
                ],
                'attr' => [
                    'inputmode' => 'numeric',
                    'autocomplete' => 'cc-csc',
                    'placeholder' => '123',
                ],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'csrf_protection' => false,
        ]);
    }
}
