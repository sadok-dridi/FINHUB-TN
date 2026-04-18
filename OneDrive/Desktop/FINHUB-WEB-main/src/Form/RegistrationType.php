<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\PasswordType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\Validator\Constraints\Email;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Regex;

class RegistrationType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('fullName', TextType::class, [
                'label' => 'Full Name',
                'constraints' => [
                    new NotBlank(['message' => 'Full name is required']),
                    new Length([
                        'min' => 3,
                        'minMessage' => 'Your full name should be at least {{ limit }} characters',
                    ]),
                ],
            ])
            ->add('email', EmailType::class, [
                'label' => 'Email Address',
                'constraints' => [
                    new NotBlank(['message' => 'Email is required']),
                    new Email(['message' => 'Please enter a valid email address']),
                ],
            ])
            ->add('password', PasswordType::class, [
                'label' => 'Password',
                'constraints' => [
                    new NotBlank(['message' => 'Password is required']),
                    new Length([
                        'min' => 8,
                        'minMessage' => 'Your password should be at least {{ limit }} characters',
                    ]),
                    new Regex([
                        'pattern' => '/[A-Z]/',
                        'message' => 'Your password must contain at least one uppercase letter',
                    ]),
                    new Regex([
                        'pattern' => '/[^a-zA-Z0-9]/',
                        'message' => 'Your password must contain at least one special character',
                    ]),
                ],
            ])
            ->add('save', SubmitType::class, [
                'label' => 'Register & Create Account'
            ])
        ;
    }
}
