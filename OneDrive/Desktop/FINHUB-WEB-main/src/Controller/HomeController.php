<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class HomeController extends AbstractController
{
    #[Route('/', name: 'app_home')]
    public function index(): Response
    {
        // If user is already logged in, maybe redirect to a specific dashboard?
        // For now, we show the landing page, which adapts its CTA buttons.
        return $this->render('home/index.html.twig');
    }
}
