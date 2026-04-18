<?php

namespace App\Controller\User;

use App\Entity\TrustedContact;
use App\Entity\User;
use App\Entity\Escrow;
use App\Entity\WalletTransaction;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

// I will just use bash to concatenate the new methods to the end of the file, replacing the broken logToBlockchain.
