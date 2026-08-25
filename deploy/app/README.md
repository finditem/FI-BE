# Application deployment configuration

The deployment workflow generates `.env.dev` and `.env.prod` from the GitHub Environment Variables and Secrets, then transfers the matching file to the application host.

Do not commit or manually create these runtime files. `TARGET` is supplied by the deployment workflow, and the runtime file contains `ECR_IMAGE_URL` together with the application configuration and secrets.
