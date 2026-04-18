<?php

namespace App\Repository;

use App\Entity\SupportTicket;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<SupportTicket>
 *
 * @method SupportTicket|null find($id, $lockMode = null, $lockVersion = null)
 * @method SupportTicket|null findOneBy(array $criteria, array $orderBy = null)
 * @method SupportTicket[]    findAll()
 * @method SupportTicket[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class SupportTicketRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, SupportTicket::class);
    }
}
