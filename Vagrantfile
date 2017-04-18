# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/xenial64"

  config.vm.network "forwarded_port", guest: 5432, host: 5432

  config.vm.provider "virtualbox" do |vb|
    vb.name = "gorilla"
    vb.cpus = "1"
    vb.memory = "512"
  end

  DB_SCRIPTS = "/vagrant/resources/oacc-db-2.0.0-rc.7/PostgreSQL_9_3"
  config.vm.provision "shell", inline: <<-SHELL
    apt-get update
    apt-get install -y --no-install-recommends postgresql
    sed -i.bak "s/^#\(listen_addresses = \).*$/\1 '*'/" /etc/postgresql/9.5/main/postgresql.conf
    echo "host oaccdb oaccuser 0.0.0.0/0 md5" >> /etc/postgresql/9.5/main/pg_hba.conf
    systemctl restart postgresql
    cd #{DB_SCRIPTS}
    sudo -u postgres sh -c 'psql -f #{DB_SCRIPTS}/create_database.sql'
    sudo -u postgres sh -c 'psql -f #{DB_SCRIPTS}/create_schema.sql oaccdb'
    sudo -u postgres sh -c 'psql -f #{DB_SCRIPTS}/create_tables.sql oaccdb'
    sudo -u postgres sh -c 'psql -f #{DB_SCRIPTS}/create_user.sql oaccdb'
  SHELL
end
