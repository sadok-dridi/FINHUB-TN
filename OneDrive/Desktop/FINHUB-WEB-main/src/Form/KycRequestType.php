<?php

namespace App\Form;

use App\Entity\KycRequest;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\NotBlank;

class KycRequestType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('documentType', ChoiceType::class, [
                'label' => 'Document Type',
                'choices'  => [
                    'Passport' => 'PASSPORT',
                    'National ID Card' => 'ID_CARD',
                    'Driver\'s License' => 'DRIVERS_LICENSE',
                ],
                'placeholder' => 'Select a document type'
            ])
            ->add('document_file', FileType::class, [
                'label' => 'Upload Document',
                'mapped' => false,
                'required' => true,
                'constraints' => [
                    new NotBlank(['message' => 'Please upload a document file.']),
                    new File([
                        'maxSize' => '5M',
                        'mimeTypes' => [
                            'image/jpeg',
                            'image/png',
                            'application/pdf',
                        ],
                        'mimeTypesMessage' => 'Please upload a valid JPEG, PNG, or PDF file',
                    ])
                ],
            ])
            ->add('save', SubmitType::class, [
                'label' => 'Submit KYC Request'
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => KycRequest::class,
        ]);
    }
}
