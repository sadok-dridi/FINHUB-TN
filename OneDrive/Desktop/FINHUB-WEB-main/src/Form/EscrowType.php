<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\Email;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Positive;

class EscrowType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('email', EmailType::class, [
                'label' => 'Recipient Email (Seller)',
                'constraints' => [
                    new NotBlank(['message' => 'Recipient email is required']),
                    new Email(['message' => 'Please enter a valid email address']),
                ],
            ])
            ->add('amount', NumberType::class, [
                'label' => 'Amount (TND)',
                'scale' => 3,
                'constraints' => [
                    new NotBlank(['message' => 'Amount is required']),
                    new Positive(['message' => 'Amount must be greater than 0']),
                ],
            ])
            ->add('conditions', TextareaType::class, [
                'label' => 'Conditions of Agreement',
                'constraints' => [
                    new NotBlank(['message' => 'Conditions are required']),
                ],
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'csrf_protection' => false,
        ]);
    }
}
