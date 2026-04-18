<?php

namespace App\Repository;

use App\Entity\VirtualCard;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<VirtualCard>
 *
 * @method VirtualCard|null find($id, $lockMode = null, $lockVersion = null)
 * @method VirtualCard|null findOneBy(array $criteria, array $orderBy = null)
 * @method VirtualCard[]    findAll()
 * @method VirtualCard[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class VirtualCardRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, VirtualCard::class);
    }

    public function save(VirtualCard $entity, bool $flush = false): void
    {
        $this->getEntityManager()->persist($entity);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    public function remove(VirtualCard $entity, bool $flush = false): void
    {
        $this->getEntityManager()->remove($entity);

        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }
}
