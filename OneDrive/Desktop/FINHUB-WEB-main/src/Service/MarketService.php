<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class MarketService
{
    private const COINGECKO_API_URL = "https://api.coingecko.com/api/v3/simple/price";
    private const ER_API_URL = "https://open.er-api.com/v6/latest/USD";
    
    // Map common symbols to their CoinGecko IDs
    private const SYMBOL_MAP = [
        'BTC' => 'bitcoin',
        'ETH' => 'ethereum',
        'SOL' => 'solana',
        'USDT' => 'tether',
        'BNB' => 'binancecoin',
        'XRP' => 'ripple',
        'DOGE' => 'dogecoin',
        'ADA' => 'cardano',
        'AVAX' => 'avalanche-2',
        'LINK' => 'chainlink',
        'MATIC' => 'matic-network',
        'DOT' => 'polkadot'
    ];

    public function __construct(
        private HttpClientInterface $client,
        private CacheInterface $cache
    ) {
    }

    /**
     * Fetches prices for an array of ticker symbols (e.g., ['BTC', 'ETH']).
     * Caches the response for 60 seconds to avoid rate matching.
     */
    public function getPrices(array $symbols): array
    {
        if (empty($symbols)) {
            return [];
        }

        // Standardize and map symbols
        $cgIds = [];
        $idToSymbol = [];
        foreach ($symbols as $symbol) {
            $upperSymbol = strtoupper($symbol);
            if (isset(self::SYMBOL_MAP[$upperSymbol])) {
                $cgId = self::SYMBOL_MAP[$upperSymbol];
                $cgIds[] = $cgId;
                $idToSymbol[$cgId] = $upperSymbol;
            } else {
                // Try using the symbol itself as a fallback lowercase ID
                $fallbackId = strtolower($upperSymbol);
                $cgIds[] = $fallbackId;
                $idToSymbol[$fallbackId] = $upperSymbol;
            }
        }

        $idsString = implode(',', array_unique($cgIds));
        $cacheKey = 'market_prices_' . md5($idsString);

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($idsString, $idToSymbol) {
            $item->expiresAfter(60); // Cache for 60 seconds

            try {
                // Request format matches CoinGecko v3 api
                $response = $this->client->request('GET', self::COINGECKO_API_URL, [
                    'query' => [
                        'ids' => $idsString,
                        'vs_currencies' => 'usd',
                        'include_24hr_change' => 'true'
                    ],
                    'timeout' => 5 // 5 second timeout to prevent hanging the dashboard
                ]);

                if ($response->getStatusCode() !== 200) {
                    return []; // Handle API errors silently by returning empty for now
                }

                $data = $response->toArray();
                $result = [];

                foreach ($data as $cgId => $metrics) {
                    if (isset($idToSymbol[$cgId])) {
                        $symbol = $idToSymbol[$cgId];
                        $result[$symbol] = [
                            'price' => $metrics['usd'] ?? 0.0,
                            'change_24h' => $metrics['usd_24h_change'] ?? 0.0
                        ];
                    }
                }

                return $result;
            } catch (\Exception $e) {
                // On failure, return empty array so app doesn't crash
                return [];
            }
        });
    }

    /**
     * Get USD to TND conversion rate
     * Caches for 1 hour since forex rates don't fluctuate as wildly
     */
    public function getUsdToTndRate(): float
    {
        return $this->cache->get('usd_tnd_rate', function (ItemInterface $item) {
            $item->expiresAfter(3600); // Cache for 1 hour

            try {
                $response = $this->client->request('GET', self::ER_API_URL, [
                    'timeout' => 5
                ]);

                if ($response->getStatusCode() === 200) {
                    $data = $response->toArray();
                    if (isset($data['rates']['TND'])) {
                        return (float) $data['rates']['TND'];
                    }
                }
            } catch (\Exception $e) {
                // Fallback fixed rate if API fails
            }
            
            return 3.15; // Safe fallback
        });
    }
}
