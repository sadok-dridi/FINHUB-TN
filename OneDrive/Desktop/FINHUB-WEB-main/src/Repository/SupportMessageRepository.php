<?php

namespace App\Repository;

use App\Entity\SupportMessage;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<SupportMessage>
 *
 * @method SupportMessage|null find($id, $lockMode = null, $lockVersion = null)
 * @method SupportMessage|null findOneBy(array $criteria, array $orderBy = null)
 * @method SupportMessage[]    findAll()
 * @method SupportMessage[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class SupportMessageRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, SupportMessage::class);
    }
}
