<?php

namespace App\Command;

use App\Repository\UserRepository;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Twig\Environment;

#[AsCommand(
    name: 'app:crypto-alerts',
    description: 'Check crypto prices and send automatic market alerts via email.',
)]
class CryptoAlertsCommand extends Command
{
    public function __construct(
        private UserRepository $userRepository,
        private MailerInterface $mailer,
        private Environment $twig
    ) {
        parent::__construct();
    }

    protected function configure(): void
    {
        $this
            ->addOption('email', null, InputOption::VALUE_OPTIONAL, 'Target a specific email address instead of broadcasting to all');
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);
        $targetEmail = $input->getOption('email');

        $io->note('Fetching live crypto prices from CoinGecko...');

        $trackedAssets = [
            ['id' => 'bitcoin', 'name' => 'Bitcoin', 'symbol' => 'BTC'],
            ['id' => 'ethereum', 'name' => 'Ethereum', 'symbol' => 'ETH'],
            ['id' => 'solana', 'name' => 'Solana', 'symbol' => 'SOL'],
            ['id' => 'ripple', 'name' => 'Ripple', 'symbol' => 'XRP'],
            ['id' => 'binancecoin', 'name' => 'Binancecoin', 'symbol' => 'BNB'],
            ['id' => 'cardano', 'name' => 'Cardano', 'symbol' => 'ADA'],
        ];

        $ids = implode(',', array_column($trackedAssets, 'id'));
        $url = "https://api.coingecko.com/api/v3/simple/price?ids={$ids}&vs_currencies=usd&include_24hr_change=true";
        
        try {
            $context = stream_context_create(['http' => ['method' => 'GET', 'timeout' => 5]]);
            $response = @file_get_contents($url, false, $context);
            if (!$response) throw new \Exception("CoinGecko API failed");
            
            $pricesData = json_decode($response, true);
        } catch (\Exception $e) {
            $io->error('Failed to connect to CoinGecko: ' . $e->getMessage());
            return Command::FAILURE;
        }

        $assetDescriptions = [
            'BTC' => 'Digital gold',
            'ETH' => 'Smart contracts platform',
            'SOL' => 'High-speed blockchain',
            'XRP' => 'Global settlements',
            'BNB' => 'Binance ecosystem',
            'ADA' => 'Peer-reviewed blockchain',
        ];

        $marketAlerts = [];
        foreach ($trackedAssets as $asset) {
            $id = $asset['id'];
            if (isset($pricesData[$id])) {
                $change = (float) $pricesData[$id]['usd_24h_change'];
                $price = number_format($pricesData[$id]['usd'], 2, '.', '');
                $formattedChange = ($change >= 0 ? '+' : '') . number_format($change, 2);
                
                $desc = $assetDescriptions[$asset['symbol']] ?? 'Digital asset';
                $reco = '';
                $type = 'HOLD';
                
                if ($change <= -5.0) {
                    $type = 'BUY';
                    $reco = "Sharp drop detected. Good opportunity for dollar-cost averaging.";
                } elseif ($change < -0.5) {
                    $type = 'BUY';
                    $reco = "Slight dip. Consider taking a small position.";
                } elseif ($change >= 5.0) {
                    $type = 'SELL';
                    $reco = "Significant surge. Might be a good time to secure profits.";
                } elseif ($change > 0.5) {
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
                    'price' => $price,
                    'change' => $formattedChange,
                    'description' => $desc,
                    'recommendation' => $reco
                ];
            }
        }

        if (empty($marketAlerts)) {
            $io->success('Markets are stable (between -0.5% and +0.5%). No alerts to send.');
            return Command::SUCCESS;
        }

        $io->text('Found ' . count($marketAlerts) . ' alert(s). Sending emails...');

        $users = $targetEmail 
            ? [$this->userRepository->findOneBy(['email' => $targetEmail])]
            : $this->userRepository->findAll();

        $sentCount = 0;
        foreach ($users as $user) {
            if (!$user || !$user->getEmail()) continue;

            $html = $this->twig->render('emails/crypto_alerts.html.twig', [
                'user' => $user,
                'alerts' => $marketAlerts
            ]);

            $email = (new Email())
                ->from('no-reply@finhub.tn')
                ->to($user->getEmail())
                ->subject('Finhub - Daily Crypto Alerts!')
                ->html($html);

            $this->mailer->send($email);
            $sentCount++;
        }

        $io->success("Successfully sent market alerts to $sentCount user(s).");
        return Command::SUCCESS;
    }
}
