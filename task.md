# EC2 Jenkins and EKS Setup Guide

## Step 1: Launch EC2 Instance

- Instance type: t2.medium
- OS: Ubuntu 24.04
- Connect to the instance via SSH

## 2. Install Jenkins on the EC2 Instance

Run the following commands on the instance:

```bash
sudo su -

apt update -y && apt install openjdk-21-jre-headless -y

java –version

wget -O /etc/apt/keyrings/jenkins-keyring.asc \
  https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc]" \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

apt-get update

apt-get install jenkins -y

## 3. aws cli

curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"

apt install unzip

unzip awscliv2.zip

sudo ./aws/install

aws configure

AWS Access Key ID [None]: YOUR_ACCESS_KEY
AWS Secret Access Key [None]: YOUR_SECRET_KEY
Default region name [None]: us-east-1
Default output format [None]: json

## 4. Setup kubectl and eksctl

kubectl version --client

curl -O https://s3.us-west-2.amazonaws.com/amazon-eks/1.33.0/2025-05-01/bin/linux/amd64/kubectl

curl -O https://s3.us-west-2.amazonaws.com/amazon-eks/1.33.0/2025-05-01/bin/linux/amd64/kubectl.sha256

sha256sum -c kubectl.sha256

chmod +x ./kubectl

mkdir -p $HOME/bin && cp ./kubectl $HOME/bin/kubectl && export PATH=$HOME/bin:$PATH

echo 'export PATH=$HOME/bin:$PATH' >> ~/.bashrc

kubectl version --client

## 5. Install eksctl

ARCH=amd64

PLATFORM=$(uname -s)_$ARCH

curl -sLO https://github.com/eksctl-io/eksctl/releases/latest/download/eksctl_$PLATFORM.tar.gz

curl -sL "https://github.com/eksctl-io/eksctl/releases/latest/download/eksctl_checksums.txt" | grep $PLATFORM | sha256sum --check

tar -xzf eksctl_$PLATFORM.tar.gz -C /tmp && rm eksctl_$PLATFORM.tar.gz

sudo mv /tmp/eksctl /usr/local/bin

eksctl version


## 6. DOCKER & Maven

apt install docker.io -y
apt install maven -y

sudo usermod -aG docker jenkins
sudo systemctl restart jenkins



## 7. Create EKS Cluster

eksctl create cluster --name cluster-name --region us-east-1 --nodegroup-name node-name --node-type t3.small --managed --nodes 2

## 8. Permissions on instance

#as a root user
sudo su - 
which kubectl
 # Example output: /root/bin/kubectl

# Move to global path
sudo cp /root/bin/kubectl /usr/local/bin/kubectl
sudo chmod +x /usr/local/bin/kubectl

aws eks update-kubeconfig --name cluster-name --region us-east-1

exit

# as a jenkins user
mkdir -p /var/lib/jenkins/.aws
mkdir -p /var/lib/jenkins/.kube

# as a root user
sudo cp -r /root/.aws/* /var/lib/jenkins/.aws/
sudo cp -r /root/.kube/* /var/lib/jenkins/.kube/
sudo chown -R jenkins:jenkins /var/lib/jenkins/.aws /var/lib/jenkins/.kube
sudo cp /root/bin/kubectl /usr/local/bin/kubectl

## 9. Jenkins Pipeline to Build, Push, and Deploy

pipeline {
    agent any

    environment {
        IMAGE1 = 'jaipalrdy/spring-boot-rest-api'
        IMAGE2 = 'jaipalrdy/task-python'
        TAG = 'latest'
        EKS_CLUSTER = 'cluster-name'
        REGION = 'us-east-1'
        PATH = "/usr/local/bin:/usr/bin:/bin:${env.PATH}"
        ECR_REGISTRY = '686255976964.dkr.ecr.us-east-1.amazonaws.com'
        REPO1 = "${ECR_REGISTRY}/jaipal/springboot-app"
        REPO2 = "${ECR_REGISTRY}/jaipal/python-app"
    }

    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }

        stage('Clone Repositories') {
            steps {
                sh '''
                    git clone -b main https://github.com/ashokitschool/spring-boot-docker-app.git springboot
                    git clone https://github.com/jpl-ry/task-python.git python
                '''
            }
        }

        stage('Build Docker Images') {
            parallel {
                stage('Build Spring Boot Image') {
                    steps {
                        dir('springboot') {
                            sh "docker build -f Dockerfile-New -t ${IMAGE1}:${TAG} ."
                        }
                    }
                }
                stage('Build Python Image') {
                    steps {
                        dir('python') {
                            sh "docker build -t ${IMAGE2}:${TAG} ."
                        }
                    }
                }
            }
        }

        stage('Push to ECR') {
            steps {
                sh """
                    aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    docker tag ${IMAGE1}:${TAG} ${REPO1}:${TAG}
                    docker tag ${IMAGE2}:${TAG} ${REPO2}:${TAG}
                    docker push ${REPO1}:${TAG}
                    docker push ${REPO2}:${TAG}
                """
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    sh """
                        aws eks --region ${REGION} update-kubeconfig --name ${EKS_CLUSTER}
                        sed -i 's|image: .*spring-boot-rest-api.*|image: ${REPO1}:${TAG}|' springboot/k8s-deployment.yml
                        sed -i 's|image: .*task-python.*|image: ${REPO2}:${TAG}|' python/python-app.yml
                        kubectl apply -f springboot/k8s-deployment.yml
                        kubectl apply -f python/python-app.yml
                    """
                }
            }
        }
    }

    post {
        always {
            echo 'Cleaning up Docker resources...'
            sh 'docker system prune -af'
        }
    }
}