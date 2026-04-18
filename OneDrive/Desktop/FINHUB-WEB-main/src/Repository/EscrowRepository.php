<?php

namespace App\Repository;

use App\Entity\Escrow;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Escrow>
 *
 * @method Escrow|null find($id, $lockMode = null, $lockVersion = null)
 * @method Escrow|null findOneBy(array $criteria, array $orderBy = null)
 * @method Escrow[]    findAll()
 * @method Escrow[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class EscrowRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Escrow::class);
    }
}
