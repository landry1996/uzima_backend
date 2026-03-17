# language: fr
@smoke
Feature: Messagerie entre utilisateurs

  En tant qu'utilisateur authentifié,
  je veux envoyer et recevoir des messages dans une conversation
  afin de communiquer avec mes contacts.

  Background:
    Given un utilisateur Alice enregistré avec le téléphone "+237622000001"
    And un utilisateur Bob enregistré avec le téléphone "+237622000002"

  @positive
  Scenario: Envoi d'un message texte
    Given Alice est authentifiée
    And une conversation entre Alice et Bob existe
    When Alice envoie le message "Bonjour Bob !"
    Then le message est enregistré dans la conversation
    And le contenu du message est "Bonjour Bob !"

  @positive
  Scenario: Récupération des messages d'une conversation
    Given Alice est authentifiée
    And une conversation entre Alice et Bob existe
    And Alice a envoyé le message "Salut !"
    When Alice récupère les messages de la conversation
    Then la liste contient au moins 1 message

  @negative
  Scenario: Message vide interdit
    Given Alice est authentifiée
    And une conversation entre Alice et Bob existe
    When Alice envoie un message vide ""
    Then une erreur de validation est retournée avec le statut HTTP 400

  @edge-case
  Scenario: Accès refusé à une conversation non participée
    Given un utilisateur Charlie enregistré avec le téléphone "+237622000003"
    And Charlie est authentifié
    And une conversation entre Alice et Bob existe
    When Charlie essaie de récupérer les messages de la conversation Alice-Bob
    Then une erreur d'autorisation est retournée avec le statut HTTP 403
