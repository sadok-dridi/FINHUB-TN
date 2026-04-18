<?php

namespace App\Repository;

use App\Entity\FinancialProfile;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<FinancialProfile>
 *
 * @method FinancialProfile|null find($id, $lockMode = null, $lockVersion = null)
 * @method FinancialProfile|null findOneBy(array $criteria, array $orderBy = null)
 * @method FinancialProfile[]    findAll()
 * @method FinancialProfile[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class FinancialProfileRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, FinancialProfile::class);
    }
}
