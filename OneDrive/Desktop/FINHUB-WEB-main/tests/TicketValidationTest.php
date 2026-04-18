<?php

use App\Kernel;
use Symfony\Bundle\FrameworkBundle\Test\KernelTestCase;

require dirname(__DIR__).'/vendor/autoload.php';

class TicketValidationTest extends KernelTestCase
{
    public function testValidation()
    {
        self::bootKernel();
        $container = self::getContainer();
        $formFactory = $container->get('form.factory');

        $form = $formFactory->createNamed('', \App\Form\TicketType::class);
        $data = ["subject" => "", "category" => "", "message" => ""];
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
