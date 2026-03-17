# language: fr
@smoke
Feature: QR Codes contextuels

  En tant qu'utilisateur,
  je veux créer et gérer des QR codes contextuels
  afin de partager mon profil ou gérer des événements.

  Background:
    Given un utilisateur propriétaire enregistré avec le téléphone "+237633000001"

  @positive
  Scenario: Création d'un QR code professionnel permanent
    Given le propriétaire est authentifié
    When il crée un QR code de type "PROFESSIONAL" sans expiration
    Then le QR code est créé avec succès
    And le QR code n'est pas révoqué

  @positive
  Scenario: Révocation d'un QR code
    Given le propriétaire est authentifié
    And un QR code professionnel existe
    When le propriétaire révoque le QR code
    Then le QR code est marqué comme révoqué

  @negative
  Scenario: QR code TEMPORARY_LOCATION sans expiration — interdit
    Given le propriétaire est authentifié
    When il tente de créer un QR code de type "TEMPORARY_LOCATION" sans expiration
    Then une erreur de validation est retournée avec le statut HTTP 400

  @edge-case
  Scenario: Idempotence de la révocation
    Given le propriétaire est authentifié
    And un QR code professionnel révoqué existe
    When le propriétaire révoque à nouveau le QR code
    Then aucune erreur n'est retournée
    And le QR code est toujours révoqué
