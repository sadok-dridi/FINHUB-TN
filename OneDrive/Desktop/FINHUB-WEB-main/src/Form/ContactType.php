<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\Email;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Regex;

class ContactType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        if ($options['include_email']) {
            $builder->add('email', EmailType::class, [
                'label' => 'Email Address',
                'constraints' => [
                    new NotBlank(['message' => 'Email address is required']),
                    new Email(['message' => 'Please enter a valid email address']),
                ],
            ]);
        }

        if ($options['include_name']) {
            $builder->add('name', TextType::class, [
                'label' => 'Contact Name',
                'constraints' => [
                    new NotBlank(['message' => 'Contact name is required']),
                    new Length(['max' => 255]),
                ],
            ]);
        }

        if ($options['include_phone']) {
            $builder->add('phone', TextType::class, [
                'label' => 'Phone Number',
                'required' => false,
                'empty_data' => '',
                'attr' => [
                    'inputmode' => 'tel',
                    'maxlength' => 20,
                    'pattern' => '^(|\+?[0-9 ]{8,20})$',
                    'placeholder' => '+216 12 345 678',
                ],
                'constraints' => [
                    new Length(['max' => 20]),
                    new Regex([
                        'pattern' => '/^$|^\+?[0-9 ]{8,20}$/',
                        'message' => 'Phone number can only contain digits, spaces, and an optional leading +.',
                    ]),
                ],
            ]);
        }
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'csrf_protection' => false,
            'include_email' => true,
            'include_name' => false,
            'include_phone' => false,
        ]);

        $resolver->setAllowedTypes('include_email', 'bool');
        $resolver->setAllowedTypes('include_name', 'bool');
        $resolver->setAllowedTypes('include_phone', 'bool');
    }
}
