# SES — transactional email only (docs/DEPLOYMENT.md §2). WhatsApp remains
# the primary coaching channel; nothing coaching-critical depends on this.
# Domain verification is a real DNS-propagation step Terraform can request
# but not complete instantly — production sending additionally requires
# AWS to move the account out of the SES sandbox, which is a support-ticket
# process, not a Terraform resource (docs/backlog L10-1104).

resource "aws_ses_domain_identity" "main" {
  domain = var.domain_name
}

resource "aws_route53_record" "ses_verification" {
  zone_id = data.aws_route53_zone.root.zone_id
  name    = "_amazonses.${var.domain_name}"
  type    = "TXT"
  ttl     = 600
  records = [aws_ses_domain_identity.main.verification_token]
}

resource "aws_ses_domain_dkim" "main" {
  domain = aws_ses_domain_identity.main.domain
}

resource "aws_route53_record" "ses_dkim" {
  count   = 3
  zone_id = data.aws_route53_zone.root.zone_id
  name    = "${aws_ses_domain_dkim.main.dkim_tokens[count.index]}._domainkey.${var.domain_name}"
  type    = "CNAME"
  ttl     = 600
  records = ["${aws_ses_domain_dkim.main.dkim_tokens[count.index]}.dkim.amazonses.com"]
}

data "aws_iam_policy_document" "ses_send" {
  statement {
    actions   = ["ses:SendEmail", "ses:SendRawEmail"]
    resources = [aws_ses_domain_identity.main.arn]
  }
}

resource "aws_iam_role_policy" "task_ses" {
  name   = "send-transactional-email"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.ses_send.json
}
