# Aircraft-and-Crew-Electronic-Market

## This is my master thesis.

In the Air Transportation domain, a disruption in an operational plan means that a flight, for some
reason (due to an irregular event) can not meet the planned schedule, causing delays or canceled
flights. Disruptions can be classified in two types, a massive one, which makes impossible to fly
safely in the affected area and leads to canceled flights, or a smaller and most frequent one, which
cause delays. Studies in this area show that airline companies lose between 2% to 3% of their
annual revenue, as consequence of these disruptions. The impact caused by small disruptions in
companies’ profits can be reduced by at least 20%, through a better Recovery Process.
Resulting from the lack of collaboration between airline companies, operation recovery from
a disruption works in a restricted solution space. To wider this space, this dissertation proposes
the usage of an Electronic Market modeled as a Multi-Agent System. Airline companies will
use the electronic market to negotiate their needs in order to apply their optimal recovery plan.
In the specific case of this dissertation, the Airline Operations Control Center (AOCC), which is
the entity responsible for dealing with irregular operations, will play the role of the buyer agent
and the other airline companies the role of seller agents. The negotiation object is an abstraction
named "Flight", which includes aircraft and crew members, based on the needs of the airline
company. The flight’s characteristics to be negotiated are cost and availability and if two flights
have the same values for those characteristics, they will be considered the same solution even if
the resources composing it are distinct.
The proposed negotiation occurs in several rounds. The AOCC (buyer agent) gives feedback over the proposals committed on each round by the sellers interested in leasing the asked
resource(s), leading to new proposals until all sellers refuse to negotiate any further. The seller
agent uses learning to calculate a new proposal in each round, through reuse similar cases of past
experiences (using Case-Based Reasoning - CBR). At the end of the negotiation, the buyer agent
selects the seller who proposed the most advantageous solution as the winner.
The main goal of the AOCC is to minimize the costs caused by a disruption, what can be
better achieved by leasing resources from other companies. In order to validate this concept and
to understand whether it is advantageous or not, an evaluation is performed to show that solutions
obtained with recourse to the electronic market are more cost-effective than solutions obtained
with the company’s own resources.

In short, this is an optimization problem in the Air Transportation Domain, trying to reduce a company costs when a flight is delayed, by trying to get the delayed resource from another company. It is related to [MASDIMA](http://masdima.com/home/) platform because the purpose of this project was to understand if a leasing contract with another company could be cheaper than a solution with its own resources.

This project was developed in JAVA SE using Intellij IDEA.
For the sake of agents representing companies, negotiation and learning, JADE was used.
The learning algorithm used was Case-based Reasoning because it seemed the best fit to the sutiation (agents learn from each round outcome).
The data used was provided by the mentors.

You can check the full report [here](https://repositorio-aberto.up.pt/handle/10216/106220) or a paper produced from this project, [here](http://www.scitepress.org/DigitalLibrary/Link.aspx?doi=10.5220/0006582401760183)
