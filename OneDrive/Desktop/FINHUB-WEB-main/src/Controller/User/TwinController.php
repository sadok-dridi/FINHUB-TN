<?php

namespace App\Controller\User;

use App\Entity\User;
use App\Entity\PortfolioItem;
use App\Entity\WalletTransaction;
use App\Repository\ExpenseRepository;
use App\Repository\FinancialProfileRepository;
use App\Repository\PortfolioItemRepository;
use App\Repository\WalletRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;
use Knp\Snappy\Pdf;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Twig\Environment;

#[IsGranted('ROLE_USER')]
#[Route('/user/twin', name: 'app_user_twin_')]
class TwinController extends AbstractController
{
    #[Route('/', name: 'index')]
    public function index(
        HttpClientInterface $client,
        CacheInterface $cache,
        WalletRepository $walletRepo,
        FinancialProfileRepository $profileRepo,
        ExpenseRepository $expenseRepo,
        PortfolioItemRepository $portfolioRepo,
        \App\Repository\ExpenseItemRepository $expenseItemRepo
    ): Response {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }
        
        $wallet = $walletRepo->findOneBy(['user' => $user]);
        $balance = $wallet ? $wallet->getBalance() : 0.0;

        $profile = $profileRepo->findOneBy(['user' => $user]);
        $income = $profile ? ($profile->getMonthlyIncome() ?? 1500) : 1500;
        $expenses = $profile ? ($profile->getMonthlyExpenses() ?? 1200) : 1200;
        $goal = $profile ? ($profile->getSavingsGoal() ?? 10000) : 10000;

        $userExpenses = $expenseRepo->findBy(['user' => $user], ['created_at' => 'DESC']);
        $totalExpenses = 0;
        $expensesData = [];
        foreach ($userExpenses as $e) {
            $totalExpenses += (float) $e->getTotalAmount();
            
            $items = $expenseItemRepo->findBy(['expense' => $e]);
            $itemsData = [];
            foreach ($items as $item) {
                $itemsData[] = [
                    'id' => $item->getId(),
                    'name' => $item->getItemName(),
                    'price' => $item->getPrice()
                ];
            }
            
            $expensesData[] = [
                'id' => $e->getId(),
                'merchant' => $e->getMerchant(),
                'amount' => $e->getTotalAmount(),
                'date' => $e->getCreatedAt()->format('Y-m-d H:i'),
                'items' => $itemsData
            ];
        }

        // Get user's current portfolio
        $portfolio = $portfolioRepo->findBy(['user' => $user]);
        $portfolioData = [];
        foreach ($portfolio as $item) {
            $portfolioData[$item->getSymbol()] = [
                'quantity' => (float) $item->getQuantity(),
                'averageCost' => (float) $item->getAverageCost()
            ];
        }
        
        // Dummy scan for the image
        if (empty($expensesData)) {
            $expensesData[] = [
                'id' => 1,
                'merchant' => 'OFFSIDE',
                'amount' => '10.50',
                'date' => date('Y-m-d H:i', strtotime('-1 hour')),
                'items' => [
                    ['id' => 1, 'name' => 'Burger', 'price' => '6.50'],
                    ['id' => 2, 'name' => 'Coke', 'price' => '4.00']
                ]
            ];
            $totalExpenses = 10.50;
        }

        // Fetch real prices from CoinGecko
        $trackedAssets = [
            ['id' => 'bitcoin', 'name' => 'Bitcoin', 'symbol' => 'BTC'],
            ['id' => 'ethereum', 'name' => 'Ethereum', 'symbol' => 'ETH'],
            ['id' => 'binancecoin', 'name' => 'Binancecoin', 'symbol' => 'BNB'],
            ['id' => 'solana', 'name' => 'Solana', 'symbol' => 'SOL'],
            ['id' => 'ripple', 'name' => 'Ripple', 'symbol' => 'XRP'],
            ['id' => 'cardano', 'name' => 'Cardano', 'symbol' => 'ADA'],
            ['id' => 'avalanche-2', 'name' => 'Avalanche', 'symbol' => 'AVAX'],
            ['id' => 'dogecoin', 'name' => 'Dogecoin', 'symbol' => 'DOGE'],
            ['id' => 'polkadot', 'name' => 'Polkadot', 'symbol' => 'DOT'],
            ['id' => 'tron', 'name' => 'Tron', 'symbol' => 'TRX'],
            ['id' => 'chainlink', 'name' => 'Chainlink', 'symbol' => 'LINK'],
            ['id' => 'shiba-inu', 'name' => 'Shiba Inu', 'symbol' => 'SHIB'],
            ['id' => 'litecoin', 'name' => 'Litecoin', 'symbol' => 'LTC'],
            ['id' => 'bitcoin-cash', 'name' => 'Bitcoin Cash', 'symbol' => 'BCH'],
            ['id' => 'uniswap', 'name' => 'Uniswap', 'symbol' => 'UNI'],
            ['id' => 'stellar', 'name' => 'Stellar', 'symbol' => 'XLM'],
            ['id' => 'monero', 'name' => 'Monero', 'symbol' => 'XMR'],
            ['id' => 'ethereum-classic', 'name' => 'Ethereum Classic', 'symbol' => 'ETC'],
            ['id' => 'near', 'name' => 'NEAR Protocol', 'symbol' => 'NEAR'],
            ['id' => 'filecoin', 'name' => 'Filecoin', 'symbol' => 'FIL'],
            ['id' => 'internet-computer', 'name' => 'Internet Computer', 'symbol' => 'ICP'],
            ['id' => 'apecoin', 'name' => 'ApeCoin', 'symbol' => 'APE'],
            ['id' => 'algorand', 'name' => 'Algorand', 'symbol' => 'ALGO'],
            ['id' => 'vechain', 'name' => 'VeChain', 'symbol' => 'VET'],
            ['id' => 'cosmos', 'name' => 'Cosmos', 'symbol' => 'ATOM'],
            ['id' => 'decentraland', 'name' => 'Decentraland', 'symbol' => 'MANA'],
            ['id' => 'the-sandbox', 'name' => 'The Sandbox', 'symbol' => 'SAND'],
            ['id' => 'aave', 'name' => 'Aave', 'symbol' => 'AAVE'],
            ['id' => 'tezos', 'name' => 'Tezos', 'symbol' => 'XTZ']
        ];

        try {
            $ids = implode(',', array_column($trackedAssets, 'id'));
            $url = "https://api.coingecko.com/api/v3/simple/price?ids={$ids}&vs_currencies=usd&include_24hr_change=true";
            
            $opts = [
                "http" => [
                    "method" => "GET",
                    "header" => "User-Agent: FINHUB-App/1.0\r\n",
                    "timeout" => 3
                ]
            ];
            $context = stream_context_create($opts);
            $response = @file_get_contents($url, false, $context);

            if ($response !== false) {
                $pricesData = json_decode($response, true);
                foreach ($trackedAssets as &$asset) {
                    $id = $asset['id'];
                    if (isset($pricesData[$id])) {
                        $asset['price'] = number_format($pricesData[$id]['usd'], $pricesData[$id]['usd'] < 0.01 ? 6 : 2, '.', '');
                        $change = $pricesData[$id]['usd_24h_change'];
                        $asset['change'] = ($change >= 0 ? '+' : '') . number_format($change, 2);
                    } else {
                        $asset['price'] = '0.00';
                        $asset['change'] = '0.00';
                    }
                }
            } else {
                // Fallback to some static data if API fails
                foreach ($trackedAssets as &$asset) {
                    $asset['price'] = '100.00';
                    $asset['change'] = '+0.00';
                }
            }
        } catch (\Exception $e) {
            foreach ($trackedAssets as &$asset) {
                $asset['price'] = '100.00';
                $asset['change'] = '+0.00';
            }
        }
        $usdToTndRate = $this->getUsdToTndRate($client, $cache);

        $assetDescriptions = [
            'BTC' => 'Digital gold',
            'ETH' => 'Smart contracts platform',
            'BNB' => 'Binance ecosystem',
            'SOL' => 'High-speed blockchain',
            'XRP' => 'Global settlements',
            'ADA' => 'Peer-reviewed blockchain',
            'AVAX' => 'Scalable dApp platform',
            'DOGE' => 'The original meme coin',
            'DOT' => 'Multi-chain interoperability',
            'TRX' => 'Decentralized content sharing',
            'LINK' => 'Decentralized oracle network',
            'SHIB' => 'Community-driven meme token',
            'LTC' => 'Fast digital silver',
            'BCH' => 'Electronic peer-to-peer cash',
            'UNI' => 'Decentralized trading protocol',
            'XLM' => 'Border-less payment network',
            'XMR' => 'Privacy-focused cryptocurrency',
            'ETC' => 'Original Ethereum blockchain',
            'NEAR' => 'Developer-friendly blockchain',
            'FIL' => 'Decentralized storage network',
            'ICP' => 'Infinite public blockchain',
            'APE' => 'Web3 economy token',
            'ALGO' => 'Pure Proof-of-Stake chain',
            'VET' => 'Supply chain management',
            'ATOM' => 'Internet of Blockchains',
            'MANA' => 'Virtual reality platform',
            'SAND' => 'Virtual gaming world',
            'AAVE' => 'Decentralized lending',
            'XTZ' => 'Self-amending blockchain'
        ];

        $marketAlerts = [];
        foreach ($trackedAssets as $asset) {
            $changeVal = (float) $asset['change'];
            $desc = $assetDescriptions[$asset['symbol']] ?? 'Digital asset';
            
            $reco = '';
            $type = 'HOLD';
            
            if ($changeVal <= -5.0) {
                $type = 'BUY';
                $reco = "Sharp drop detected. Good opportunity for dollar-cost averaging.";
            } elseif ($changeVal < -0.5) {
                $type = 'BUY';
                $reco = "Slight dip. Consider taking a small position.";
            } elseif ($changeVal >= 5.0) {
                $type = 'SELL';
                $reco = "Significant surge. Might be a good time to secure profits.";
            } elseif ($changeVal > 0.5) {
                $type = 'SELL';
                $reco = "Positive momentum. Keep an eye on resistance levels.";
            } else {
                $type = 'HOLD';
                $reco = "Consolidating. Hold your current positions.";
            }

            $marketAlerts[] = [
                'type' => $type,
                'symbol' => $asset['symbol'],
                'name' => $asset['name'],
                'id' => $asset['id'],
                'price' => $asset['price'],
                'change' => $asset['change'],
                'description' => $desc,
                'recommendation' => $reco
            ];
        }

        return $this->render('user/twin.html.twig', [
            'walletBalance' => $balance,
            'income' => $income,
            'expenses' => $expenses,
            'goal' => $goal,
            'expensesList' => $expensesData,
            'totalExpenses' => $totalExpenses,
            'marketAssets' => $trackedAssets,
            'portfolio' => $portfolioData,
            'usdToTndRate' => $usdToTndRate,
            'marketAlerts' => $marketAlerts
        ]);
    }

    private function getUsdToTndRate(HttpClientInterface $client, CacheInterface $cache): float
    {
        return $cache->get('usd_tnd_rate', function (ItemInterface $item) use ($client) {
            $item->expiresAfter(3600);
            try {
                $response = $client->request('GET', 'https://open.er-api.com/v6/latest/USD');
                $data = $response->toArray();
                return (float) ($data['rates']['TND'] ?? 3.12);
            } catch (\Exception $e) {
                return 3.12;
            }
        });
    }

    #[Route('/trade', name: 'trade', methods: ['POST'])]
    public function trade(
        Request $request,
        EntityManagerInterface $em,
        WalletRepository $walletRepo,
        PortfolioItemRepository $portfolioRepo,
        HttpClientInterface $client,
        CacheInterface $cache,
        \App\Service\WalletService $walletService
    ): JsonResponse {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return new JsonResponse(['success' => false, 'message' => 'Unauthorized'], 401);
        }

        $data = json_decode($request->getContent(), true);
        if (!$data) {
            return new JsonResponse(['success' => false, 'message' => 'Invalid data'], 400);
        }

        $symbol = $data['symbol'] ?? ''; // Java uses raw ID e.g., 'bitcoin', 'binancecoin'
        $type = strtoupper($data['type'] ?? '');
        $amount = (float)($data['amount'] ?? 0);
        $priceUsd = (float)($data['price'] ?? 0);

        if (!$symbol || !in_array($type, ['BUY', 'SELL']) || $amount <= 0 || $priceUsd <= 0) {
            return new JsonResponse(['success' => false, 'message' => 'Invalid parameters'], 400);
        }

        $usdToTndRate = $this->getUsdToTndRate($client, $cache);
        $totalUsd = $amount * $priceUsd;
        $totalCostTnd = round($totalUsd * $usdToTndRate, 2);

        $wallet = $walletRepo->findOneBy(['user' => $user]);
        if (!$wallet) {
            return new JsonResponse(['success' => false, 'message' => 'Wallet not found'], 404);
        }

        $currentBalance = (float)$wallet->getBalance();
        $portfolioItem = $portfolioRepo->findOneBy(['user' => $user, 'symbol' => $symbol]);

        $txType = ($type === 'BUY') ? 'DEBIT' : 'CREDIT';
        // Need to find the mapped symbol for reference (e.g. BTC, ETH) or just use strtoupper of id
        $displaySymbol = strtoupper($symbol);
        $txRef = "MARKET " . ($type === 'BUY' ? 'BUY' : 'SELL') . " " . $displaySymbol;
        $txAmountAbs = number_format(abs($totalCostTnd), 3, '.', '');

        if ($type === 'BUY') {
            if ($currentBalance < $totalCostTnd) {
                return new JsonResponse(['success' => false, 'message' => 'Insufficient funds. Cost: ' . $totalCostTnd . ' TND'], 400);
            }

            // Update Wallet
            $newBalance = $currentBalance - $totalCostTnd;
            $wallet->setBalance((string)$newBalance);

            // Update Portfolio
            if ($portfolioItem) {
                $currentQty = (float)$portfolioItem->getQuantity();
                $currentAvgCost = (float)$portfolioItem->getAverageCost();
                
                $oldTotalUsd = $currentQty * $currentAvgCost;
                $newTotalUsd = $oldTotalUsd + $totalUsd;
                $newQty = $currentQty + $amount;
                $newAvgCost = round($newTotalUsd / $newQty, 4);
                
                $portfolioItem->setQuantity((string)$newQty);
                $portfolioItem->setAverageCost((string)$newAvgCost);
            } else {
                $portfolioItem = new PortfolioItem();
                $portfolioItem->setUser($user);
                $portfolioItem->setSymbol($symbol);
                $portfolioItem->setQuantity((string)$amount);
                $portfolioItem->setAverageCost((string)round($priceUsd, 4));
                $em->persist($portfolioItem);
            }

        } else {
            // SELL
            if (!$portfolioItem || (float)$portfolioItem->getQuantity() < $amount) {
                return new JsonResponse(['success' => false, 'message' => 'Insufficient asset quantity'], 400);
            }

            // Update Wallet
            $newBalance = $currentBalance + $totalCostTnd;
            $wallet->setBalance((string)$newBalance);

            // Update Portfolio
            $currentQty = (float)$portfolioItem->getQuantity();
            $newQty = $currentQty - $amount;
            
            if ($newQty <= 0.00000001) { // Floating point precision threshold
                $em->remove($portfolioItem);
                $portfolioItem = null;
            } else {
                $portfolioItem->setQuantity((string)$newQty);
            }
        }

        // --- WalletTransaction exact Java replication ---
        $walletService->recordTransaction($wallet, $txType, $txAmountAbs, $txRef);

        $em->flush();

        // --- SimulatedTrade Database Entry ---
        $now = new \DateTimeImmutable('now', new \DateTimeZone('Africa/Tunis'));
        $trade = new \App\Entity\SimulatedTrade();
        $trade->setUser($user);
        $trade->setAssetSymbol($symbol);
        $trade->setAction($type);
        $trade->setQuantity((string)$amount);
        $trade->setPriceAtTransaction((string)$priceUsd);
        // $trade->setTotalCost((string)$totalCostTnd); // It's an auto-generated column in the DB
        // Using regular \DateTime since the entity expects \DateTimeInterface but the property uses DATETIME_MUTABLE
        $tradeDate = new \DateTime($now->format('Y-m-d H:i:s'));
        $trade->setTransactionDate($tradeDate);
        
        $em->persist($trade);
        $em->flush();

        return new JsonResponse([
            'success' => true,
            'message' => "Successfully {$type} $amount $displaySymbol",
            'newBalance' => $newBalance,
            'portfolio' => $portfolioItem && clone $portfolioItem ? [
                'quantity' => (float)$portfolioItem->getQuantity(),
                'averageCost' => (float)$portfolioItem->getAverageCost()
            ] : null
        ]);
    }

    #[Route('/expense/add', name: 'expense_add', methods: ['POST'])]
    public function addExpense(Request $request, EntityManagerInterface $em, \Symfony\Component\Form\FormFactoryInterface $formFactory): JsonResponse
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return new JsonResponse(['success' => false, 'message' => 'Unauthorized'], 401);
        }

        $data = json_decode($request->getContent(), true);

        $form = $formFactory->createNamed('', \App\Form\ExpenseType::class);
        $form->submit($data);

        if (!$form->isValid()) {
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
            return new JsonResponse(['success' => false, 'errors' => $errors]);
        }

        $merchant = $form->get('merchant')->getData();
        $amount = (float) $form->get('amount')->getData();
        $itemsData = $data['items'] ?? []; // we can parse items manually or via form, let's just use raw data to keep it simple since items might not be perfectly mapped by ID index

        $expense = new \App\Entity\Expense();
        $expense->setUser($user);
        $expense->setMerchant($merchant);
        $expense->setTotalAmount((string)$amount);
        $expense->setCreatedAt(new \DateTime());

        $em->persist($expense);

        $savedItems = [];
        foreach ($itemsData as $itemData) {
            $itemName = $itemData['name'] ?? '';
            $itemPrice = (float)($itemData['price'] ?? 0);

            if ($itemName && $itemPrice >= 0) {
                $expenseItem = new \App\Entity\ExpenseItem();
                $expenseItem->setExpense($expense);
                $expenseItem->setItemName($itemName);
                $expenseItem->setPrice((string)$itemPrice);
                $em->persist($expenseItem);

                $savedItems[] = [
                    'name' => $itemName,
                    'price' => $itemPrice
                ];
            }
        }

        $em->flush();

        return new JsonResponse([
            'success' => true,
            'expense' => [
                'id' => $expense->getId(),
                'merchant' => $expense->getMerchant(),
                'amount' => $expense->getTotalAmount(),
                'date' => $expense->getCreatedAt()->format('Y-m-d H:i'),
                'items' => $savedItems
            ]
        ]);
    }

    #[Route('/expense/edit/{id}', name: 'expense_edit', methods: ['PUT', 'POST'])]
    public function editExpense(
        int $id,
        Request $request,
        EntityManagerInterface $em,
        ExpenseRepository $expenseRepo,
        \App\Repository\ExpenseItemRepository $expenseItemRepo,
        \Symfony\Component\Form\FormFactoryInterface $formFactory
    ): JsonResponse {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return new JsonResponse(['success' => false, 'message' => 'Unauthorized'], 401);
        }

        $expense = $expenseRepo->find($id);
        if (!$expense || $expense->getUser() !== $user) {
            return new JsonResponse(['success' => false, 'message' => 'Expense not found'], 404);
        }

        $data = json_decode($request->getContent(), true);

        $form = $formFactory->createNamed('', \App\Form\ExpenseType::class);
        $form->submit($data);

        if (!$form->isValid()) {
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
            return new JsonResponse(['success' => false, 'errors' => $errors]);
        }

        $merchant = $form->get('merchant')->getData();
        $amount = (float) $form->get('amount')->getData();
        $itemsData = $data['items'] ?? [];

        // Adjust Total Expenses Difference
        $oldAmount = (float)$expense->getTotalAmount();
        $expense->setMerchant($merchant);
        $expense->setTotalAmount((string)$amount);

        // Remove old items
        $oldItems = $expenseItemRepo->findBy(['expense' => $expense]);
        foreach ($oldItems as $oldItem) {
            $em->remove($oldItem);
        }

        $em->persist($expense);

        // Add new items
        $savedItems = [];
        foreach ($itemsData as $itemData) {
            $itemName = $itemData['name'] ?? '';
            $itemPrice = (float)($itemData['price'] ?? 0);

            if ($itemName && $itemPrice >= 0) {
                $expenseItem = new \App\Entity\ExpenseItem();
                $expenseItem->setExpense($expense);
                $expenseItem->setItemName($itemName);
                $expenseItem->setPrice((string)$itemPrice);
                $em->persist($expenseItem);

                $savedItems[] = [
                    'name' => $itemName,
                    'price' => $itemPrice
                ];
            }
        }

        $em->flush();

        return new JsonResponse([
            'success' => true,
            'expense' => [
                'id' => $expense->getId(),
                'merchant' => $expense->getMerchant(),
                'amount' => $expense->getTotalAmount(),
                'date' => $expense->getCreatedAt()->format('Y-m-d H:i'),
                'items' => $savedItems
            ],
            'oldAmount' => $oldAmount
        ]);
    }

    #[Route('/expense/{id}', name: 'expense_delete', methods: ['DELETE'])]
    public function deleteExpense(int $id, EntityManagerInterface $em, ExpenseRepository $expenseRepo): JsonResponse
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return new JsonResponse(['success' => false, 'message' => 'Unauthorized'], 401);
        }

        $expense = $expenseRepo->find($id);
        if (!$expense || $expense->getUser() !== $user) {
            return new JsonResponse(['success' => false, 'message' => 'Expense not found'], 404);
        }

        $em->remove($expense);
        $em->flush();

        return new JsonResponse(['success' => true]);
    }

    #[Route('/expense/email', name: 'expense_email', methods: ['POST'])]
    public function emailExpenses(
        Request $request,
        ExpenseRepository $expenseRepo,
        \App\Repository\ExpenseItemRepository $expenseItemRepo,
        Pdf $pdf,
        MailerInterface $mailer,
        Environment $twig
    ): JsonResponse {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return new JsonResponse(['success' => false, 'message' => 'Unauthorized'], 401);
        }

        $data = json_decode($request->getContent(), true);
        $customMessage = $data['message'] ?? 'Please find attached your expense summary.';
        $recipientEmail = $data['email'] ?? $user->getEmail();

        if (empty($recipientEmail) || !filter_var($recipientEmail, FILTER_VALIDATE_EMAIL)) {
            return new JsonResponse(['success' => false, 'message' => 'Valid email address is required'], 400);
        }

        $userExpenses = $expenseRepo->findBy(['user' => $user], ['created_at' => 'DESC']);
        $totalExpenses = 0;
        $expensesData = [];
        
        foreach ($userExpenses as $e) {
            $totalExpenses += (float) $e->getTotalAmount();
            
            $items = $expenseItemRepo->findBy(['expense' => $e]);
            $itemsData = [];
            foreach ($items as $item) {
                $itemsData[] = [
                    'name' => $item->getItemName(),
                    'price' => $item->getPrice()
                ];
            }
            
            $expensesData[] = [
                'merchant' => $e->getMerchant(),
                'amount' => $e->getTotalAmount(),
                'date' => $e->getCreatedAt()->format('Y-m-d H:i'),
                'items' => $itemsData
            ];
        }

        try {
            $html = $twig->render('user/pdf_expenses.html.twig', [
                'user' => $user,
                'expenses' => $expensesData,
                'totalAmount' => $totalExpenses,
            ]);

            $pdfContent = $pdf->getOutputFromHtml($html);

            $email = (new Email())
                ->from('no-reply@finhub.tn')
                ->to($recipientEmail)
                ->subject('Your Financial Twin Expense Summary')
                ->html('<p>' . nl2br(htmlspecialchars($customMessage)) . '</p>')
                ->attach($pdfContent, 'expenses_summary.pdf', 'application/pdf');

            $mailer->send($email);

        } catch (\Exception $e) {
            // Note: If wkhtmltopdf is missing, it will throw an exception here
            return new JsonResponse(['success' => false, 'message' => 'Error generating/sending email: ' . $e->getMessage()], 500);
        }

        return new JsonResponse(['success' => true, 'message' => 'Email sent successfully!']);
    }
}
