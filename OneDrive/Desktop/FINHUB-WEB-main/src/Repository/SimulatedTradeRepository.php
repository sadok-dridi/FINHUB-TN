<?php

namespace App\Repository;

use App\Entity\SimulatedTrade;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<SimulatedTrade>
 *
 * @method SimulatedTrade|null find($id, $lockMode = null, $lockVersion = null)
 * @method SimulatedTrade|null findOneBy(array $criteria, array $orderBy = null)
 * @method SimulatedTrade[]    findAll()
 * @method SimulatedTrade[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class SimulatedTradeRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, SimulatedTrade::class);
    }
}