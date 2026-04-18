<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CollectionType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Positive;
use Symfony\Component\Validator\Constraints\Length;

class ExpenseType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('merchant', TextType::class, [
                'constraints' => [
                    new NotBlank(['message' => 'Merchant name is required.']),
                    new Length(['max' => 255, 'maxMessage' => 'Merchant name is too long.'])
                ]
            ])
            ->add('amount', NumberType::class, [
                'scale' => 2,
                'constraints' => [
                    new NotBlank(['message' => 'Total amount is required.']),
                    new Positive(['message' => 'Amount must be greater than zero.'])
                ]
            ])
            ->add('items', CollectionType::class, [
                'entry_type' => ExpenseItemType::class,
                'allow_add' => true,
                'allow_delete' => true,
                'by_reference' => false,
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
