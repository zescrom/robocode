package es.us.lsi;

import static java.lang.Math.PI;

import java.awt.Color;

import robocode.AdvancedRobot;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class ThugTank extends AdvancedRobot {
	// Constante para mantener alg�n criterio durante un n�mero de turnos.
	private static final int HOLD_TURNS = 10;

	// Distancia a la que se considera cercano un enemigo, para cambiar de
	// l�gica.
	private static final double PROXIMITY_DISTANCE = 200.0d;

	// El tanque se mover�, como m�ximo, a una velocidad entre la mitad de la
	// velocidad m�xima
	// // a hasta la velocidad m�xima.
	// private static final double HALF_MAX_VELOCITY = Rules.MAX_VELOCITY /
	// 2.0d;

	// Direcci�n del movimiento del tanque.
	int myDirection = 1;

	// Datos del campo de batalla.
	double width;
	double height;
	double margin = 20.0d;

	// Contador de los turnos en los que no se ejecutar�n movimientos
	int turnsToNextMarginsAnalysis = HOLD_TURNS;

	public void run() {
		// Establecemos los colores del tanque.
		setGunColor(new Color(205, 190, 30));
		setBulletColor(new Color(205, 190, 30));
		setRadarColor(new Color(255, 255, 255));
		setScanColor(new Color(255, 255, 255));
		setBodyColor(new Color(200, 160, 0));

		// Establecemos que el cuerpo del tanque y la torreta con el ca��n y el
		// radar sean independientes.
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		// Mantenemos el radar girando continuamente.
		turnRadarLeftRadians(Double.NEGATIVE_INFINITY);

		// Obtenemos el tama�o del campo de batalla para establecer un l�mite de
		// movimiento.
		width = this.getBattleFieldWidth() - margin;
		height = this.getBattleFieldHeight() - margin;
	}

	/**
	 * M�todo encargado de actuar cuando se detecta un tanque enemigo.
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		// Sumamos las trayectorias, para ver si estamos alineados con el tanque
		// enemigo.
		double bearing = getHeadingRadians() + e.getBearingRadians();
		// Intentamos calcular la velocidad del enemigo para saber d�nde
		// disparar.
		double enemySpeed = Math.sin(e.getHeadingRadians() - bearing) * e.getVelocity();
		setTurnRadarRightRadians(-getRadarTurnRemainingRadians());
		// Giramos el ca��n para ajustar el tiro.
		// TODO: Afinar
		double cannonTurn = normalize(bearing - getGunHeadingRadians() + enemySpeed / 10);
		setTurnGunRightRadians(cannonTurn);

		// Por defecto, nos moveremos hacia delante.
		boolean ahead = true;
		// Tendremos un comportamiento distinto en funci�n de la proximidad al
		// enemigo.
		if (e.getDistance() > PROXIMITY_DISTANCE) {
			// Estamos lejos, intentamos aproximarnos al enemigo.
			setTurnRightRadians(normalize(bearing - getHeadingRadians() + enemySpeed / getVelocity()));
		} else {
			// Estamos cerca, nos posicionamos para poder movernos �gilmente.
			setTurnLeft(-90 - e.getBearing());
			// Nos movemos hacia atr�s.
			ahead = false;
		}
		move(ahead, myDirection * (e.getDistance() - (PROXIMITY_DISTANCE - 10.0d)));
		setFire(Math.min(3, Math.ceil(400.0d / e.getDistance())));
	}

	private static double normalize(double angle) {
		return (angle %= (2 * PI)) >= 0 ? (angle < PI) ? angle : angle - (2 * PI)
				: (angle >= -PI) ? angle : angle + (2 * PI);
	}

	private void move(boolean ahead, double distance) {
		// Obtengo mi posici�n.
		double xPos = this.getX() + distance;
		double yPos = this.getY() + distance;

		// Vemos si toca analizar si estamos cerca de las paredes.
		if (turnsToNextMarginsAnalysis <= 0) {
			// �Hay que tener cuidado con las paredes!.
			if (xPos <= margin || yPos <= margin || xPos >= width || yPos >= height) {
				// Estamos en zona peligrosa, cambiamos de sentido de
				// movimiento.
				ahead = !ahead;
				// Reiniciamos el contador para el siguiente an�lisis de mi
				// posici�n.
				turnsToNextMarginsAnalysis = HOLD_TURNS;
			}

			if (ahead)
				setAhead(distance);
			else
				setBack(distance);
		} else {
			turnsToNextMarginsAnalysis--;
		}

		// changeMaxVelocity();
	}

	// private void changeMaxVelocity() {
	// // Cambiamos de velocidad para enga�ar al tanque enemigo.
	// double newSpeed = (HALF_MAX_VELOCITY * Math.random()) +
	// HALF_MAX_VELOCITY;
	// // Por si acaso...
	// if (newSpeed < 3.0d)
	// newSpeed = 5.0d;
	// setMaxVelocity(newSpeed);
	// }

	/**
	 * M�todo para hacer el baile de la victoria.
	 */
	public void onWin(WinEvent e) {
		for (int i = 0; i < 50; i++) {
			ahead(10);
			back(10);
		}
	}

	/**
	 * M�todo para cambiar de direcci�n si se choca con la pared.
	 */
	public void onHitWall(HitWallEvent e) {
		myDirection = -myDirection;
		turnsToNextMarginsAnalysis = HOLD_TURNS;
	}
}
