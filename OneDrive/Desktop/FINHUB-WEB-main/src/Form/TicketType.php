<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Length;

class TicketType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('subject', TextType::class, [
                'constraints' => [
                    new NotBlank(['message' => 'Subject is required.']),
                    new Length(['max' => 255, 'maxMessage' => 'Subject is too long.'])
                ]
            ])
            ->add('category', ChoiceType::class, [
                'choices' => [
                    'Wallet' => 'WALLET',
                    'Transaction' => 'TRANSACTION',
                    'Security' => 'SECURITY',
                    'Other' => 'OTHER'
                ],
                'constraints' => [
                    new NotBlank(['message' => 'Category is required.'])
                ]
            ])
            ->add('message', TextareaType::class, [
                'constraints' => [
                    new NotBlank(['message' => 'Message is required.']),
                    new Length(['min' => 10, 'minMessage' => 'Message is too short.'])
                ]
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
