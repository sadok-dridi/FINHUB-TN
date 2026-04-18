# First copy the private key to the PHP project
cp /home/sadok/Documents/projects/FINHUB-TN/docusign_private.key /home/sadok/Documents/projects/PIDEV/FINHUB-WEB/

# Add env vars
echo "
DOCUSIGN_CLIENT_ID=3ba10e4d-aebe-4da6-abcd-a161e9a58c24
DOCUSIGN_USER_ID=94ae06ad-36c0-4d6c-9bac-f8c77aed2989
DOCUSIGN_API_ACCOUNT_ID=79d8e470-4e7c-4d87-9b47-87d79485fb0c
DOCUSIGN_BASE_PATH=https://demo.docusign.net/restapi
DOCUSIGN_PRIVATE_KEY_PATH=docusign_private.key" >> /home/sadok/Documents/projects/PIDEV/FINHUB-WEB/.env
