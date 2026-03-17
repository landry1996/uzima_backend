# language: fr
@smoke
Feature: Paiement entre utilisateurs

  En tant qu'utilisateur authentifié,
  je veux envoyer de l'argent à un autre utilisateur
  afin de régler des transactions sans quitter l'application.

  Background:
    Given un utilisateur expéditeur enregistré avec le téléphone "+237611000001"
    And un utilisateur destinataire enregistré avec le téléphone "+237611000002"

  @positive
  Scenario: Paiement Mobile Money réussi
    Given l'expéditeur est authentifié
    When il envoie 5000 XAF au destinataire via MOBILE_MONEY avec la description "Loyer mars"
    Then la transaction est créée avec le statut "COMPLETED"
    And l'identifiant de transaction est retourné

  @positive
  Scenario: Paiement sans description
    Given l'expéditeur est authentifié
    When il envoie 1000 XAF au destinataire via MOBILE_MONEY sans description
    Then la transaction est créée avec le statut "COMPLETED"

  @negative
  Scenario: Auto-paiement interdit
    Given l'expéditeur est authentifié
    When il essaie de s'envoyer 500 XAF à lui-même
    Then une erreur "SELF_PAYMENT" est retournée avec le statut HTTP 400

  @negative
  Scenario: Montant nul interdit
    Given l'expéditeur est authentifié
    When il envoie 0 XAF au destinataire via MOBILE_MONEY avec la description "Zéro"
    Then une erreur de validation est retournée avec le statut HTTP 400

  @positive
  Scenario: Annulation d'une transaction PENDING
    Given l'expéditeur est authentifié
    And une transaction PENDING existe pour l'expéditeur
    When l'expéditeur annule la transaction
    Then la transaction est annulée avec le statut "CANCELLED"

  @edge-case
  Scenario: Description de 255 caractères — limite acceptée
    Given l'expéditeur est authentifié
    When il envoie 500 XAF au destinataire via MOBILE_MONEY avec une description de 255 caractères
    Then la transaction est créée avec le statut "COMPLETED"
