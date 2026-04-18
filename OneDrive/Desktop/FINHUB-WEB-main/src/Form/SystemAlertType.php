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

class SystemAlertType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('source', TextType::class, [
                'constraints' => [
                    new NotBlank(['message' => 'Source is required.']),
                    new Length(['max' => 255, 'maxMessage' => 'Source is too long.'])
                ]
            ])
            ->add('severity', ChoiceType::class, [
                'choices' => [
                    'Info' => 'INFO',
                    'Warning' => 'WARNING',
                    'Critical' => 'CRITICAL',
                    'Error' => 'ERROR'
                ],
                'constraints' => [
                    new NotBlank(['message' => 'Severity is required.'])
                ]
            ])
            ->add('message', TextareaType::class, [
                'constraints' => [
                    new NotBlank(['message' => 'Message is required.']),
                    new Length(['min' => 5, 'minMessage' => 'Message must be at least 5 characters.'])
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
