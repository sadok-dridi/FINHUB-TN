<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Length;

class KnowledgeBaseType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'constraints' => [
                    new NotBlank(['message' => 'Article title is required.']),
                    new Length(['max' => 255, 'maxMessage' => 'Title is too long.'])
                ]
            ])
            ->add('category', TextType::class, [
                'constraints' => [
                    new NotBlank(['message' => 'Category is required.']),
                    new Length(['max' => 100, 'maxMessage' => 'Category is too long.'])
                ]
            ])
            ->add('content', TextareaType::class, [
                'constraints' => [
                    new NotBlank(['message' => 'Content is required.']),
                    new Length(['min' => 10, 'minMessage' => 'Content must be at least 10 characters long.'])
                ]
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'csrf_protection' => false,
            'allow_extra_fields' => true,
        ]);
    }
}
