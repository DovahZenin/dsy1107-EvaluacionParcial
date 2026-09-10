# Security Group para RDS PostgreSQL
resource "aws_security_group" "rds_sg" {
  name        = "dsy1107-rds-sg"
  description = "Permitir trafico PostgreSQL"

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # En produccion se restringe a las subnets del backend/ECS
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Instancia RDS PostgreSQL
resource "aws_db_instance" "postgres" {
  allocated_storage      = 20
  max_allocated_storage  = 20
  db_name                = "solicitudes_db"
  engine                 = "postgres"
  engine_version         = "15"
  instance_class         = "db.t3.micro"
  username               = "postgres"
  password               = "postgres123" # Puedes parametrizarlo si prefieres
  skip_final_snapshot    = true
  publicly_accessible    = true
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
}

output "db_endpoint" {
  value       = aws_db_instance.postgres.endpoint
  description = "Endpoint de la base de datos RDS PostgreSQL"
}