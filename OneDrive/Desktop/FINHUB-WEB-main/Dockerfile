# Use PHP 8.2 with Apache
FROM php:8.2-apache

# Install required system packages and PHP extensions for Symfony
RUN apt-get update && apt-get install -y \
    git \
    unzip \
    libicu-dev \
    && docker-php-ext-install pdo pdo_mysql intl

# Enable Apache mod_rewrite for Symfony routing
RUN a2enmod rewrite

# Change Apache document root to Symfony's public/ directory
ENV APACHE_DOCUMENT_ROOT /var/www/html/public
RUN sed -ri -e 's!/var/www/html!${APACHE_DOCUMENT_ROOT}!g' /etc/apache2/sites-available/*.conf
RUN sed -ri -e 's!/var/www/!${APACHE_DOCUMENT_ROOT}!g' /etc/apache2/apache2.conf /etc/apache2/conf-available/*.conf

# Install Composer
COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

# Set working directory
WORKDIR /var/www/html

# Set environment variables for production build
ENV APP_ENV=prod
ENV APP_DEBUG=0

# Copy the project files
COPY . .

# Install PHP dependencies without dev packages
RUN composer install --no-dev --optimize-autoloader --no-scripts
RUN php bin/console importmap:install
RUN php bin/console cache:clear


# Set permissions for Symfony cache and logs
RUN mkdir -p var/cache/prod var/log
RUN chown -R www-data:www-data var/
