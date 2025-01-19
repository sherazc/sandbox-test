#############
# install Docker
#############
sudo -i
apt update && apt upgrade -y
apt install docker.io
sudo rm /usr/local/bin/docker-compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.32.1/docker-compose-linux-aarch64" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

#############
# install JDK
#############

mkdir -p /opt/dev/jdk
cd /opt/dev/jdk
wget https://download.java.net/java/GA/jdk23.0.1/c28985cbf10d4e648e4004050f8781aa/11/GPL/openjdk-23.0.1_linux-aarch64_bin.tar.gz
tar -xvzf openjdk-23.0.1_linux-aarch64_bin.tar.gz

sudo mv /opt/dev/jdk/jdk-23.0.1 /opt/
cd ..
rm -rf /opt/dev/jdk/jdk
nano /etc/profile
# Add below lines at the end of /etc/profile file
# export JAVA_HOME=/opt/jdk-23.0.1
# export MY_PATH="$JAVA_HOME/bin"
# export PATH=$MY_PATH:$PATH
update-alternatives --install /usr/bin/java java /opt/jdk-23.0.1/bin/java 100
update-alternatives --install /usr/bin/javac javac /opt/jdk-23.0.1/bin/javac 100
update-alternatives --display java
update-alternatives --display javac
