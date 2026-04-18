<?php

namespace App\Form;

use App\Entity\VirtualCard;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\NotBlank;

class AdminVirtualCardType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('status', ChoiceType::class, [
                'label' => 'Card Status',
                'choices' => [
                    'Active' => 'ACTIVE',
                    'Frozen' => 'FROZEN',
                    'Expired' => 'EXPIRED',
                ],
                'constraints' => [
                    new NotBlank(['message' => 'Card status is required']),
                ],
            ])
            ->add('expiryDate', DateType::class, [
                'label' => 'Expiration Date',
                'widget' => 'single_text',
                'input' => 'datetime',
                'constraints' => [
                    new NotBlank(['message' => 'Expiration date is required']),
                ],
            ])
            ->add('save', SubmitType::class, [
                'label' => 'Update Card',
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => VirtualCard::class,
        ]);
    }
}
