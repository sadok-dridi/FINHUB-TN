<?php

use App\Kernel;
use Symfony\Bundle\FrameworkBundle\Test\KernelTestCase;

require dirname(__DIR__).'/vendor/autoload.php';

class FormValidationTest extends KernelTestCase
{
    public function testValidation()
    {
        self::bootKernel();
        $container = self::getContainer();
        $formFactory = $container->get('form.factory');

        $form = $formFactory->createNamed('', \App\Form\ExpenseType::class);
        $data = ["merchant" => "", "amount" => null, "items" => [["name" => "", "price" => null]]];
        $form->submit($data);

        $errors = [];
        foreach ($form->getErrors(true) as $error) {
            $path = '';
            $parent = $error->getOrigin();
            while ($parent && $parent->getName() !== '') {
                if ($path === '') {
                    $path = $parent->getName();
                } else {
                    $path = $parent->getName() . '.' . $path;
                }
                $parent = $parent->getParent();
            }
            $errors[$path] = $error->getMessage();
        }
        
        echo json_encode(['success' => false, 'errors' => $errors]);
    }
}
