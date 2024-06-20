insert into public.es_operation_template (id, placeholder, language, title, message, attributes, ui, result_texts)
values
    (1, 'authorize_payment', 'en', 'Payment Approval', 'Please confirm the payment', '[
  {
    "id": "operation.amount",
    "type": "AMOUNT",
    "text": "Amount",
    "params": {
      "amount": "amount",
      "currency": "currency"
    }
  },
  {
    "id": "operation.account",
    "type": "KEY_VALUE",
    "text": "To Account",
    "params": {
      "value": "iban"
    }
  }
]', null,
 '{
  "success": "Payment of ${amount} ${currency} was confirmed",
  "reject": "Payment was rejected",
  "failure": "Payment approval failed"
}');
